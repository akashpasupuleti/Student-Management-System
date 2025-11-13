package com.dailycodework.excel2database.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * This class handles the migration of plain text passwords to hashed passwords.
 * It runs once on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordMigrationRunner implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Starting password migration check...");
        
        try {
            // Get all databases
            List<String> databases = jdbcTemplate.queryForList(
                    "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME NOT IN ('mysql', 'information_schema', 'performance_schema', 'sys')",
                    String.class);
            
            for (String db : databases) {
                migratePasswordsInDatabase(db);
            }
            
            log.info("Password migration check completed.");
        } catch (Exception e) {
            log.error("Error during password migration: {}", e.getMessage(), e);
        }
    }
    
    private void migratePasswordsInDatabase(String dbName) {
        log.info("Checking database: {}", dbName);
        
        try {
            // Switch to the database
            jdbcTemplate.execute("USE " + dbName);
            
            // Get all tables
            List<String> tables = jdbcTemplate.queryForList("SHOW TABLES", String.class);
            
            // Process student tables
            tables.stream()
                    .filter(table -> table.endsWith("_students"))
                    .forEach(this::migratePasswordsInTable);
            
            // Process teacher tables
            tables.stream()
                    .filter(table -> table.endsWith("_teachers"))
                    .forEach(this::migratePasswordsInTable);
            
        } catch (Exception e) {
            log.error("Error processing database {}: {}", dbName, e.getMessage());
        }
    }
    
    private void migratePasswordsInTable(String tableName) {
        try {
            log.info("Checking table: {}", tableName);
            
            // Get all users with non-hashed passwords (assuming hashed passwords start with $2a$)
            List<Object[]> users = jdbcTemplate.query(
                    "SELECT id, password FROM " + tableName + " WHERE password NOT LIKE '$2a$%'",
                    (rs, rowNum) -> new Object[]{rs.getInt("id"), rs.getString("password")}
            );
            
            if (users.isEmpty()) {
                log.info("No plain text passwords found in table: {}", tableName);
                return;
            }
            
            log.info("Found {} plain text passwords in table: {}", users.size(), tableName);
            
            // Update each password
            for (Object[] user : users) {
                int id = (int) user[0];
                String plainPassword = (String) user[1];
                String hashedPassword = passwordEncoder.encode(plainPassword);
                
                jdbcTemplate.update(
                        "UPDATE " + tableName + " SET password = ? WHERE id = ?",
                        hashedPassword, id
                );
            }
            
            log.info("Successfully migrated {} passwords in table: {}", users.size(), tableName);
            
        } catch (Exception e) {
            log.error("Error migrating passwords in table {}: {}", tableName, e.getMessage());
        }
    }
}
