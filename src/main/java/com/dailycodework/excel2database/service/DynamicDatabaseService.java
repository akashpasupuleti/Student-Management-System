package com.dailycodework.excel2database.service;

import com.dailycodework.excel2database.domain.Subject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DynamicDatabaseService {

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    private JdbcTemplate mainJdbcTemplate;

    // List of system databases to exclude when listing available databases
    private static final List<String> SYSTEM_DATABASES = List.of(
            "mysql", "information_schema", "performance_schema", "sys"
    );

    // Store the current college context
    private String currentCollege;

    /**
     * ✅ Get JdbcTemplate for the main database.
     * Instead of creating a new database for each college, we'll use a single database
     * and create tables with college name as prefix.
     */
    public JdbcTemplate getJdbcTemplateForCollege(String collegeName) {
        // Store the current college context
        this.currentCollege = collegeName;

        // Initialize the main JDBC template if it's null
        if (mainJdbcTemplate == null) {
            log.info("🔧 Initializing main JdbcTemplate");
            try {
                DataSource dataSource = createDataSource(dbUrl);
                // Test the connection before creating the JdbcTemplate
                try (java.sql.Connection conn = dataSource.getConnection()) {
                    if (conn.isValid(5)) { // 5 second timeout
                        log.info("✅ Database connection test successful");
                    } else {
                        log.error("❌ Database connection test failed - connection is invalid");
                        throw new RuntimeException("Database connection test failed");
                    }
                }

                mainJdbcTemplate = new JdbcTemplate(dataSource);
                mainJdbcTemplate.setQueryTimeout(10); // 10 seconds timeout for queries
                log.info("✅ Main database connection established.");
            } catch (Exception e) {
                log.error("❌ Failed to initialize main database connection: {}", e.getMessage(), e);
                // Create a custom runtime exception that will be caught by the global exception handler
                throw new RuntimeException("Failed to connect to database. Please check your database configuration.", e);
            }
        }
        return mainJdbcTemplate;
    }

    /**
     * Get the current college context
     */
    public String getCurrentCollege() {
        return currentCollege;
    }

    /**
     * ✅ Create a new DataSource for the provided JDBC URL.
     */
    private DataSource createDataSource(String jdbcUrl) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setUrl(jdbcUrl);
        ds.setUsername(dbUsername);
        ds.setPassword(dbPassword);
        return ds;
    }

    /**
     * ✅ Fetch subjects for a student from a dynamic table by HTNO.
     */
    public List<Subject> getSubjectsFromDynamicTableByHtno(String college, String tableName, String htno) {
        JdbcTemplate jdbc = getJdbcTemplateForCollege(college);

        try {
            // Add college prefix to table name
            String collegePrefix = college.toLowerCase() + "_";
            String fullTableName = collegePrefix + tableName;

            log.info("Looking for student {} in table {}", htno, fullTableName);

            // Sanitize the full table name for SQL query
            String sanitizedTableName = sanitizeTableName(fullTableName);

            // First check if the table exists
            List<String> tables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, fullTableName);
            if (tables.isEmpty()) {
                log.warn("⚠️ Table {} does not exist", fullTableName);
                // Return an empty list but not List.of() to allow modifications
                return new ArrayList<>();
            }

            // Then check if the student has records in this table
            String countQuery = "SELECT COUNT(*) FROM " + sanitizedTableName + " WHERE htno = ?";
            log.debug("Executing query: {}", countQuery);

            Integer count = jdbc.queryForObject(countQuery, Integer.class, htno);
            log.debug("Count for HTNO {} in table {}: {}", htno, fullTableName, count);

            if (count == null || count == 0) {
                log.warn("⚠️ No records found for student {} in table {}", htno, fullTableName);
                // Check if the HTNO exists in the students table
                String studentsTable = college.toLowerCase() + "_students";
                String sanitizedStudentsTable = sanitizeTableName(studentsTable);
                try {
                    Integer studentExists = jdbc.queryForObject("SELECT COUNT(*) FROM " + sanitizedStudentsTable + " WHERE htno = ?", Integer.class, htno);
                    if (studentExists != null && studentExists > 0) {
                        log.info("Student {} exists in students table but has no results in {}", htno, fullTableName);
                        // Return an empty list but don't return null to prevent errors
                        return new ArrayList<>(); // Return empty list but not List.of() to allow modifications
                    }
                } catch (Exception ex) {
                    log.warn("Error checking student existence: {}", ex.getMessage());
                }
                return List.of();
            }

            String selectQuery = "SELECT * FROM " + sanitizedTableName + " WHERE htno = ?";
            log.debug("Executing query: {}", selectQuery);

            List<Subject> results = jdbc.query(
                    selectQuery,
                    (rs, rowNum) -> {
                        Subject s = new Subject();
                        s.setSno(rs.getInt("sno"));
                        s.setHtno(rs.getString("htno"));
                        s.setSubcode(rs.getString("subcode"));
                        s.setSubname(rs.getString("subname"));
                        s.setInternals(rs.getInt("internals"));
                        s.setGrade(rs.getString("grade"));
                        s.setCredit(rs.getDouble("credit"));
                        return s;
                    },
                    htno
            );

            log.info("Found {} results for student {} in table {}", results.size(), htno, fullTableName);
            return results;
        } catch (Exception e) {
            log.warn("⚠️ Could not fetch results from {}: {}", tableName, e.getMessage());
            return new ArrayList<>(); // Return empty list but not List.of() to allow modifications
        }
    }

    /**
     * ✅ Fetch all subjects from a department-semester table for a college.
     */
    public List<Subject> getAllSubjectsFromDynamicTable(String college, String dept, String semester) {
        JdbcTemplate jdbc = getJdbcTemplateForCollege(college);
        String baseTable = "results_" + dept.toLowerCase() + "_" + semester.replace("-", "_");
        String table = college.toLowerCase() + "_" + baseTable;

        try {
            log.info("Getting all subjects from table {}", table);

            // First check if the table exists
            List<String> tables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, table);
            if (tables.isEmpty()) {
                log.warn("⚠️ Table {} does not exist", table);
                return new ArrayList<>();
            }

            // Sanitize table name to prevent SQL injection
            String sanitizedTableName = sanitizeTableName(table);
            String query = "SELECT * FROM " + sanitizedTableName;
            log.debug("Executing query: {}", query);

            List<Subject> results = jdbc.query(query, (rs, rowNum) -> {
                Subject s = new Subject();
                s.setSno(rs.getInt("sno"));
                s.setHtno(rs.getString("htno"));
                s.setSubcode(rs.getString("subcode"));
                s.setSubname(rs.getString("subname"));
                s.setInternals(rs.getInt("internals"));
                s.setGrade(rs.getString("grade"));
                s.setCredit(rs.getDouble("credit"));
                return s;
            });

            log.info("Found {} subjects in table {}", results.size(), table);
            return results;
        } catch (Exception e) {
            log.error("❌ Error reading from table {}: {}", table, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * ✅ Get all colleges from the tables in the main database.
     * This method extracts college names from table prefixes.
     */
    public List<String> getAllDatabases() {
        try {
            JdbcTemplate jdbc = getJdbcTemplateForCollege(null);
            List<String> tables = jdbc.queryForList("SHOW TABLES", String.class);

            log.debug("Found {} tables in database: {}", tables.size(), tables);

            // Extract college names from table prefixes
            List<String> colleges = tables.stream()
                    .map(table -> {
                        int underscoreIndex = table.indexOf('_');
                        return underscoreIndex > 0 ? table.substring(0, underscoreIndex) : null;
                    })
                    .filter(college -> college != null && !college.isEmpty() && !SYSTEM_DATABASES.contains(college))
                    .distinct()
                    .toList();

            log.info("Found {} colleges in database: {}", colleges.size(), colleges);

            // If no colleges found, add a default one
            if (colleges.isEmpty()) {
                log.warn("No colleges found in database, adding default college 'jntuh'");
                // Create a mutable list and add the default college
                List<String> collegesWithDefault = new ArrayList<>();
                collegesWithDefault.add("jntuh");
                return collegesWithDefault;
            }

            return colleges;
        } catch (Exception e) {
            log.error("❌ Error getting colleges from database: {}", e.getMessage(), e);
            // Return a default college in case of error
            List<String> defaultColleges = new ArrayList<>();
            defaultColleges.add("jntuh");
            return defaultColleges;
        }
    }

    /**
     * Sanitize table name to prevent SQL injection
     * This method ensures that table names only contain alphanumeric characters,
     * underscores, and are properly escaped with backticks.
     */
    public String sanitizeTableName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }

        // Log the original table name for debugging
        log.debug("Sanitizing table name: {}", tableName);

        // Remove any existing backticks from the input
        String cleanName = tableName.replace("`", "");

        // Only allow alphanumeric characters and underscores
        String sanitized = cleanName.replaceAll("[^a-zA-Z0-9_]", "");

        // If sanitization removed all characters, throw an exception
        if (sanitized.isEmpty()) {
            log.error("Invalid table name after sanitization: {}", tableName);
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        // Wrap the sanitized name in backticks for MySQL
        String result = "`" + sanitized + "`";
        log.debug("Sanitized table name: {} -> {}", tableName, result);
        return result;
    }

    /**
     * ✅ Check if a student has results in any semester
     */
    public List<String> getAvailableSemesters(String college, String department, String htno) {
        JdbcTemplate jdbc = getJdbcTemplateForCollege(college);
        List<String> availableSemesters = new ArrayList<>();

        try {
            // Get all tables in the database
            String collegePrefix = college.toLowerCase() + "_";
            String tablePattern = collegePrefix + "results_" + department.toLowerCase() + "_%";

            log.info("Looking for tables matching pattern: {}", tablePattern);
            List<String> tables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, tablePattern);
            log.info("Found {} tables matching pattern: {}", tables.size(), tables);

            for (String table : tables) {
                try {
                    // Extract semester from table name
                    String semesterPart = table.substring(table.lastIndexOf("_") + 1);
                    String formattedSemester = semesterPart.replace("_", "-");

                    // Sanitize table name for SQL query
                    String sanitizedTable = sanitizeTableName(table);

                    // Check if student has results in this semester
                    String query = "SELECT COUNT(*) FROM " + sanitizedTable + " WHERE htno = ?";
                    log.debug("Executing query: {} with HTNO: {}", query, htno);

                    Integer count = jdbc.queryForObject(query, Integer.class, htno);
                    log.debug("Count for HTNO {} in table {}: {}", htno, table, count);

                    if (count != null && count > 0) {
                        log.info("Adding semester {} for HTNO {} (table: {})", formattedSemester, htno, table);
                        availableSemesters.add(formattedSemester);
                    }
                } catch (Exception e) {
                    log.warn("Error checking table {}: {}", table, e.getMessage());
                }
            }

            // If no semesters found, try to find any semester where the student has results
            if (availableSemesters.isEmpty()) {
                log.info("No semesters found for HTNO {} in department {}. Trying all departments...", htno, department);

                // Try all common departments
                for (String dept : List.of("CSE", "CSD", "CSM", "ECE", "EEE", "MECH", "IT", "CIVIL", "CS")) {
                    if (dept.equalsIgnoreCase(department)) continue; // Skip the one we already tried

                    String deptTablePattern = collegePrefix + "results_" + dept.toLowerCase() + "_%";
                    List<String> deptTables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, deptTablePattern);

                    for (String table : deptTables) {
                        try {
                            // Extract semester from table name
                            String semesterPart = table.substring(table.lastIndexOf("_") + 1);
                            String formattedSemester = semesterPart.replace("_", "-");

                            // Sanitize table name for SQL query
                            String sanitizedTable = sanitizeTableName(table);

                            // Check if student has results in this semester
                            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + sanitizedTable + " WHERE htno = ?", Integer.class, htno);
                            if (count != null && count > 0) {
                                log.info("Found semester {} for HTNO {} in department {} (table: {})", formattedSemester, htno, dept, table);
                                availableSemesters.add(formattedSemester);
                            }
                        } catch (Exception e) {
                            // Skip this table
                        }
                    }

                    if (!availableSemesters.isEmpty()) {
                        break; // Stop once we find results in any department
                    }
                }
            }

            // Also check for SGPA/CGPA data in the grades table
            try {
                String gradesTable = college.toLowerCase() + "_grades_" + department.toLowerCase();
                List<String> gradesTables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, gradesTable);

                if (!gradesTables.isEmpty()) {
                    log.info("Found grades table: {}", gradesTable);
                    String sanitizedTable = sanitizeTableName(gradesTable);

                    // Get all columns that start with sem_
                    List<String> columns = jdbc.queryForList(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_NAME = ? AND COLUMN_NAME LIKE 'sem_%'",
                        String.class, gradesTable);

                    log.info("Found {} semester columns in grades table: {}", columns.size(), columns);

                    // Check each semester column for this student
                    for (String column : columns) {
                        try {
                            String semesterPart = column.substring(4); // Remove 'sem_' prefix
                            String formattedSemester = semesterPart.replace("_", "-");

                            // Check if student has SGPA for this semester
                            String query = "SELECT " + column + " FROM " + sanitizedTable + " WHERE htno = ?";
                            Double sgpa = jdbc.queryForObject(query, Double.class, htno);

                            if (sgpa != null && sgpa > 0) {
                                log.info("Found SGPA {} for semester {} for HTNO {}", sgpa, formattedSemester, htno);
                                if (!availableSemesters.contains(formattedSemester)) {
                                    availableSemesters.add(formattedSemester);
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Error checking SGPA for column {}: {}", column, e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking grades table: {}", e.getMessage());
            }

            log.info("Available semesters for HTNO {}: {}", htno, availableSemesters);
            return availableSemesters;
        } catch (Exception e) {
            log.error("❌ Error getting available semesters: {}", e.getMessage(), e);
            return availableSemesters;
        }
    }

    /**
     * ✅ Check if a student exists in the students table
     */
    public boolean studentExistsInStudentsTable(String college, String htno) {
        JdbcTemplate jdbc = getJdbcTemplateForCollege(college);
        String tableName = college.toLowerCase() + "_students";
        String table = sanitizeTableName(tableName);

        try {
            // First check if the table exists
            List<String> tables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, tableName);
            if (tables.isEmpty()) {
                log.debug("Table {} does not exist", tableName);
                return false;
            }

            // Then check if the student exists in this table
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE htno = ?", Integer.class, htno);
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("Error checking if student exists: {}", e.getMessage());
            return false;
        }
    }
}
