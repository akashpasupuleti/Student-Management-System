package com.dailycodework.excel2database.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final DynamicDatabaseService dynamicDbService;
    private final PasswordEncoder passwordEncoder;

    /**
     * ✅ Register student to dynamically named table based on college name.
     */
    public boolean registerStudent(String fname, String lname, String htno, String email, String password, String college) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
        String tableName = college.toLowerCase() + "_students";
        String table = dynamicDbService.sanitizeTableName(tableName);

        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "fname VARCHAR(100)," +
                "lname VARCHAR(100)," +
                "htno VARCHAR(100) UNIQUE," +
                "email VARCHAR(150)," +
                "password VARCHAR(100)," +
                "reset_token VARCHAR(255))");

        List<String> existing = jdbc.queryForList("SELECT htno FROM " + table + " WHERE htno = ?", String.class, htno);
        if (!existing.isEmpty()) return false;

        jdbc.update("INSERT INTO " + table + " (fname, lname, htno, email, password) VALUES (?, ?, ?, ?, ?)",
                fname, lname, htno, email, passwordEncoder.encode(password));
        return true;
    }

    /**
     * ✅ Validate student login using HTNO + Password from correct college.
     */
    public boolean validateStudent(String htno, String password, String college) {
        if (htno == null || htno.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            log.warn("❌ Invalid login parameters: HTNO or password is null or empty");
            return false;
        }

        // If college is null or empty, try to detect it
        if (college == null || college.trim().isEmpty()) {
            log.info("College not provided for HTNO {}, attempting to detect", htno);
            college = detectCollegeForHtno(htno);
            if (college == null) {
                log.warn("❌ Could not detect college for HTNO {}", htno);
                return false;
            }
            log.info("Detected college {} for HTNO {}", college, htno);
        }

        try {
            JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
            String tableName = college.toLowerCase() + "_students";
            String table = dynamicDbService.sanitizeTableName(tableName);

            // First check if the table exists
            List<String> tables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, tableName);
            if (tables.isEmpty()) {
                log.warn("Table {} does not exist in database for college {}", tableName, college);

                // Try to find the correct college if the table doesn't exist
                String detectedCollege = detectCollegeForHtno(htno);
                if (detectedCollege != null && !detectedCollege.equals(college)) {
                    log.info("Trying with detected college {} instead of {}", detectedCollege, college);
                    return validateStudent(htno, password, detectedCollege);
                }

                return false;
            }

            String sql = "SELECT password FROM " + table + " WHERE htno = ?";
            String storedPassword = jdbc.queryForObject(sql, String.class, htno);

            boolean matches = passwordEncoder.matches(password, storedPassword);
            if (!matches) {
                log.info("Password mismatch for HTNO {} in college {}", htno, college);
            } else {
                log.info("✅ Successfully validated HTNO {} in college {}", htno, college);
            }

            return matches;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // This is expected if the student doesn't exist
            log.info("No student found with HTNO {} in college {}", htno, college);

            // Try to find the correct college if the student doesn't exist in this one
            String detectedCollege = detectCollegeForHtno(htno);
            if (detectedCollege != null && !detectedCollege.equals(college)) {
                log.info("Trying with detected college {} instead of {}", detectedCollege, college);
                return validateStudent(htno, password, detectedCollege);
            }

            return false;
        } catch (Exception e) {
            log.error("❌ Login validation failed for {} in college {}: {}", htno, college, e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ Detect the correct college (database) based on HTNO by scanning all databases.
     */
    public String detectCollegeForHtno(String htno) {
        if (htno == null || htno.trim().isEmpty()) {
            log.warn("❌ Empty or null HTNO provided");
            return null;
        }

        try {
            List<String> colleges = dynamicDbService.getAllDatabases();
            log.info("Searching for HTNO {} in {} colleges: {}", htno, colleges.size(), colleges);

            if (colleges.isEmpty()) {
                log.warn("❌ No colleges found in the database");
                // Fallback to a default college if no colleges are found
                log.info("Using default college 'jntuh' as fallback");
                return "jntuh";
            }

            JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(null);

            // First, try to find in student tables
            for (String college : colleges) {
                try {
                    // Check if the students table exists
                    String studentsTable = college.toLowerCase() + "_students";
                    String sanitizedTable = dynamicDbService.sanitizeTableName(studentsTable);
                    List<String> tables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, studentsTable);

                    log.debug("Checking table {} for HTNO {}", studentsTable, htno);
                    if (!tables.isEmpty()) {
                        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + sanitizedTable + " WHERE htno = ?", Integer.class, htno);
                        log.debug("Count for HTNO {} in table {}: {}", htno, studentsTable, count);
                        if (count != null && count > 0) {
                            log.info("✅ HTNO {} found in students table for college: {}", htno, college);
                            return college;
                        }
                    } else {
                        log.debug("Table {} does not exist", studentsTable);
                    }
                } catch (Exception e) {
                    // Log at info level to help diagnose issues
                    log.info("⚠️ Skipping students table for college {} due to error: {}", college, e.getMessage());
                }
            }

            // If not found in student tables, try to find in results tables
            for (String college : colleges) {
                try {
                    // Look for result tables with this college prefix
                    String resultsPattern = college.toLowerCase() + "_results_%";
                    List<String> tables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, resultsPattern);

                    log.debug("Found {} results tables for college {}: {}", tables.size(), college, tables);
                    if (!tables.isEmpty()) {
                        for (String table : tables) {
                            try {
                                String sanitizedTable = dynamicDbService.sanitizeTableName(table);
                                Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + sanitizedTable + " WHERE htno = ?", Integer.class, htno);
                                log.debug("Count for HTNO {} in table {}: {}", htno, table, count);
                                if (count != null && count > 0) {
                                    log.info("✅ HTNO {} found in results table {} for college: {}", htno, table, college);
                                    return college;
                                }
                            } catch (Exception e) {
                                // Skip this table and try the next one
                                log.debug("Could not query table {} for college {}: {}", table, college, e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    // Log at info level to help diagnose issues
                    log.info("⚠️ Skipping results tables for college {} due to error: {}", college, e.getMessage());
                }
            }

            log.warn("❌ HTNO {} not found in any college", htno);

            // If we have at least one college, use the first one as a fallback
            if (!colleges.isEmpty()) {
                String defaultCollege = colleges.get(0);
                log.info("Using first available college '{}' as fallback for HTNO {}", defaultCollege, htno);
                return defaultCollege;
            }

            // If all else fails, use a hardcoded default
            log.info("Using hardcoded default college 'jntuh' as last resort fallback");
            return "jntuh";
        } catch (Exception e) {
            log.error("❌ Error detecting college for HTNO {}: {}", htno, e.getMessage(), e);
            // Even if there's an error, try to return a default college
            log.info("Using default college 'jntuh' due to error");
            return "jntuh";
        }
    }

    /**
     * ✅ Get registered email using HTNO by checking all *_students tables.
     */
    public String getEmailByHtno(String htno) {
        String college = detectCollegeForHtno(htno);
        if (college == null) return null;

        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
        String tableName = college.toLowerCase() + "_students";
        String table = dynamicDbService.sanitizeTableName(tableName);

        try {
            return jdbc.queryForObject("SELECT email FROM " + table + " WHERE htno = ?", String.class, htno);
        } catch (Exception e) {
            log.debug("Could not find email for HTNO {} in table {}: {}", htno, tableName, e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Store reset token using HTNO + Email.
     */
    public boolean storeResetTokenByHtnoAndEmail(String htno, String email, String token) {
        String college = detectCollegeForHtno(htno);
        if (college == null) return false;

        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
        String tableName = college.toLowerCase() + "_students";
        String table = dynamicDbService.sanitizeTableName(tableName);

        try {
            int updated = jdbc.update("UPDATE " + table +
                    " SET reset_token = ? WHERE LOWER(htno) = LOWER(?) AND LOWER(email) = LOWER(?)",
                    token, htno, email);
            return updated > 0;
        } catch (Exception e) {
            log.error("❌ Failed to store reset token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Get the reset token for a given HTNO.
     */
    public String getResetTokenByHtno(String htno) {
        String college = detectCollegeForHtno(htno);
        if (college == null) return null;

        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
        String tableName = college.toLowerCase() + "_students";
        String table = dynamicDbService.sanitizeTableName(tableName);

        try {
            return jdbc.queryForObject("SELECT reset_token FROM " + table + " WHERE LOWER(htno) = LOWER(?)", String.class, htno);
        } catch (Exception e) {
            log.debug("No reset token found for HTNO {}: {}", htno, e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Reset password using token and clear token.
     */
    public boolean updatePassword(String token, String newPassword) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(null);
        List<String> colleges = dynamicDbService.getAllDatabases();

        for (String college : colleges) {
            try {
                String tableName = college.toLowerCase() + "_students";
                String table = dynamicDbService.sanitizeTableName(tableName);

                int updated = jdbc.update("UPDATE " + table +
                        " SET password = ?, reset_token = NULL WHERE reset_token = ?",
                        passwordEncoder.encode(newPassword), token);
                if (updated > 0) {
                    log.info("✅ Password updated successfully for token in college {}", college);
                    return true;
                }
            } catch (Exception e) {
                log.warn("⚠️ Password reset check failed for college {}: {}", college, e.getMessage());
            }
        }
        return false;
    }

    /**
     * ✅ Store token fallback if email + college is known.
     */
    public boolean storeResetToken(String email, String token, String college) {
        try {
            JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
            String tableName = college.toLowerCase() + "_students";
            String table = dynamicDbService.sanitizeTableName(tableName);

            int updated = jdbc.update("UPDATE " + table + " SET reset_token = ? WHERE email = ?", token, email);
            return updated > 0;
        } catch (Exception e) {
            log.error("❌ Reset token store failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Get student details by HTNO.
     */
    public com.dailycodework.excel2database.model.Student getStudentByHtno(String college, String htno) {
        try {
            JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
            String tableName = college.toLowerCase() + "_students";
            String table = dynamicDbService.sanitizeTableName(tableName);

            return jdbc.queryForObject("SELECT * FROM " + table + " WHERE htno = ?",
                (rs, rowNum) -> {
                    com.dailycodework.excel2database.model.Student student = new com.dailycodework.excel2database.model.Student();
                    student.setId(rs.getLong("id"));
                    student.setFname(rs.getString("fname"));
                    student.setLname(rs.getString("lname"));
                    student.setHtno(rs.getString("htno"));
                    student.setEmail(rs.getString("email"));
                    // Try to determine department from results tables
                    String dept = findDepartmentForHtno(htno, college);
                    student.setDepartment(dept != null ? dept : "CSE"); // Default to CSE if not found
                    return student;
                }, htno);
        } catch (Exception e) {
            log.warn("⚠️ Could not get student details for HTNO {} in college {}: {}", htno, college, e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Find department for a student by checking all result tables.
     */
    private String findDepartmentForHtno(String htno, String college) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
        String collegePrefix = college.toLowerCase() + "_results_";

        // Check all common departments
        for (String dept : List.of("CSE", "CSD", "CSM", "ECE", "EEE", "MECH", "IT", "CIVIL", "CS")) {
            try {
                // Check if any semester table for this department contains this HTNO
                for (String sem : List.of("1_1", "1_2", "2_1", "2_2", "3_1", "3_2", "4_1", "4_2")) {
                    String tableName = collegePrefix + dept.toLowerCase() + "_" + sem;
                    String table = dynamicDbService.sanitizeTableName(tableName);

                    try {
                        // Check if table exists
                        List<String> tables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, tableName);
                        if (tables.isEmpty()) continue;

                        // Check if HTNO exists in this table
                        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE htno = ?", Integer.class, htno);
                        if (count != null && count > 0) {
                            return dept;
                        }
                    } catch (Exception e) {
                        // Skip this table and try the next one
                        continue;
                    }
                }
            } catch (Exception e) {
                // Skip this department and try the next one
                continue;
            }
        }

        return null;
    }
}
