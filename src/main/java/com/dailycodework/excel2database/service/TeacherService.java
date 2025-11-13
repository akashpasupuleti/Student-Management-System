package com.dailycodework.excel2database.service;

import com.dailycodework.excel2database.domain.Teacher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherService {

    private final DynamicDatabaseService dynamicDbService;
    private final PasswordEncoder passwordEncoder;

    public boolean registerTeacher(Teacher teacher, String college) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
        String dept = teacher.getDepartment().toLowerCase();
        String tableName = college.toLowerCase() + "_" + dept + "_teachers";
        String table = dynamicDbService.sanitizeTableName(tableName);

        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "firstname VARCHAR(100), " +
                "lastname VARCHAR(100), " +
                "email VARCHAR(150) UNIQUE, " +
                "department VARCHAR(100), " +
                "password VARCHAR(100), " +
                "role VARCHAR(20), " +
                "reset_token VARCHAR(255))");

        if ("HOD".equalsIgnoreCase(teacher.getRole())) {
            List<String> hodExists = jdbc.queryForList("SELECT email FROM " + table + " WHERE role = 'HOD'", String.class);
            if (!hodExists.isEmpty()) return false;
        }

        List<String> exists = jdbc.queryForList("SELECT email FROM " + table + " WHERE email = ?", String.class, teacher.getEmail());
        if (!exists.isEmpty()) return false;

        jdbc.update("INSERT INTO " + table + " (firstname, lastname, email, department, password, role) VALUES (?, ?, ?, ?, ?, ?)",
                teacher.getFirstName(), teacher.getLastName(), teacher.getEmail(),
                teacher.getDepartment(), passwordEncoder.encode(teacher.getPassword()), teacher.getRole());

        return true;
    }

    public boolean validateTeacher(String firstName, String password, String college, String department) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
        String tableName = college.toLowerCase() + "_" + department.toLowerCase() + "_teachers";
        String table = dynamicDbService.sanitizeTableName(tableName);

        try {
            String stored = jdbc.queryForObject("SELECT password FROM " + table + " WHERE firstname = ?", String.class, firstName);
            return stored != null && passwordEncoder.matches(password, stored);
        } catch (Exception e) {
            return false;
        }
    }

    public String findHodEmail(String college, String department) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
        String tableName = college.toLowerCase() + "_" + department.toLowerCase() + "_teachers";
        String table = dynamicDbService.sanitizeTableName(tableName);

        try {
            return jdbc.queryForObject("SELECT email FROM " + table + " WHERE role = 'HOD' LIMIT 1", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String[] findCollegeAndDeptByFirstName(String firstName) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(null);
        List<String> tables = jdbc.queryForList("SHOW TABLES LIKE '%_teachers'", String.class);

        for (String tableName : tables) {
            try {
                String table = dynamicDbService.sanitizeTableName(tableName);
                String query = "SELECT firstname FROM " + table + " WHERE firstname = ?";
                String match = jdbc.queryForObject(query, String.class, firstName);

                if (match != null && match.equals(firstName)) {
                    // Extract college and department from table name
                    // Format: college_dept_teachers
                    String[] parts = tableName.split("_");
                    if (parts.length >= 3) {
                        String college = parts[0];
                        String dept = parts[1];
                        return new String[]{college, dept};
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    public String getEmailByUsername(String username) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(null);
        List<String> tables = jdbc.queryForList("SHOW TABLES LIKE '%_teachers'", String.class);

        for (String tableName : tables) {
            try {
                String table = dynamicDbService.sanitizeTableName(tableName);
                String sql = "SELECT email FROM " + table + " WHERE LOWER(firstname) = LOWER(?)";
                String email = jdbc.queryForObject(sql, String.class, username);
                if (email != null) {
                    return email;
                }
            } catch (Exception ignored) {
                // ignore if not found in this table
            }
        }
        return null;
    }

    public void storeResetToken(String email, String token) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(null);
        List<String> tables = jdbc.queryForList("SHOW TABLES LIKE '%_teachers'", String.class);

        for (String tableName : tables) {
            try {
                String table = dynamicDbService.sanitizeTableName(tableName);
                int updated = jdbc.update("UPDATE " + table + " SET reset_token = ? WHERE email = ?", token, email);
                if (updated > 0) return;
            } catch (Exception ignored) {}
        }
    }

    public boolean updatePasswordByToken(String token, String newPassword) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(null);
        List<String> tables = jdbc.queryForList("SHOW TABLES LIKE '%_teachers'", String.class);

        for (String tableName : tables) {
            try {
                String table = dynamicDbService.sanitizeTableName(tableName);
                String sql = "UPDATE " + table + " SET password = ?, reset_token = NULL WHERE reset_token = ?";
                int updated = jdbc.update(sql, passwordEncoder.encode(newPassword), token);
                if (updated > 0) {
                    log.info("✅ Password updated successfully in table: {}", tableName);
                    return true;
                }
            } catch (Exception e) {
                log.warn("⚠️ Error updating in table {}: {}", tableName, e.getMessage());
            }
        }
        return false;
    }

}
