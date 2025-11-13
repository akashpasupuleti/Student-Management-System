package com.dailycodework.excel2database.util;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseUtil {

    private static final Map<String, JdbcTemplate> jdbcMap = new ConcurrentHashMap<>();

    public static JdbcTemplate getJdbcTemplateForCollege(String college) {
        // Use a single database for all colleges
        if (!jdbcMap.containsKey("main")) {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            ds.setUrl("jdbc:mysql://localhost:3306/test1"); // Use the main database
            ds.setUsername("root");
            ds.setPassword("root");
            jdbcMap.put("main", new JdbcTemplate(ds));
        }
        return jdbcMap.get("main");
    }

    public static String sanitizeTableName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }

        // Remove any backticks and sanitize the table name
        // Only allow alphanumeric characters and underscores
        String sanitized = tableName.replaceAll("[^a-zA-Z0-9_]", "");

        // If sanitization removed all characters, throw an exception
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        // Wrap the sanitized name in backticks for MySQL
        return "`" + sanitized + "`";
    }
}

