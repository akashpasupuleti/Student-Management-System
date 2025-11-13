package com.dailycodework.excel2database.service;

import com.dailycodework.excel2database.domain.Subject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SgpaService {

    private final DynamicDatabaseService dynamicDbService;

    public void calculateAndStoreAllSGPAForSemester(List<Subject> subjects, String dept, String semester, String college) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
        List<String> uniqueHtnoList = subjects.stream().map(Subject::getHtno).distinct().toList();

        for (String htno : uniqueHtnoList) {
            List<Subject> filtered = subjects.stream().filter(s -> s.getHtno().equals(htno)).toList();
            double sgpa = calculateSGPAFromList(filtered);
            saveSgpaToDynamicTable(jdbc, htno, semester, sgpa, dept);
        }
    }

    public void saveSgpaToDynamicTable(JdbcTemplate jdbc, String htno, String semester, double sgpa, String dept) {
        // Get the college from the JDBC URL
        String college = extractCollegeFromJdbc(jdbc);
        String baseTable = "grades_" + dept.toLowerCase();
        String table = college.toLowerCase() + "_" + baseTable;
        String column = "sem_" + semester.replace("-", "_");

        log.info("Saving SGPA {} for HTNO {} in table {}", sgpa, htno, table);

        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " (" +
                "htno VARCHAR(20) PRIMARY KEY, " +
                "sem_1_1 DECIMAL(4,2), sem_1_2 DECIMAL(4,2), " +
                "sem_2_1 DECIMAL(4,2), sem_2_2 DECIMAL(4,2), " +
                "sem_3_1 DECIMAL(4,2), sem_3_2 DECIMAL(4,2), " +
                "sem_4_1 DECIMAL(4,2), sem_4_2 DECIMAL(4,2), " +
                "cgpa DECIMAL(4,2))");

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE htno = ?", Integer.class, htno);
        if (count != null && count > 0) {
            jdbc.update("UPDATE " + table + " SET " + column + " = ? WHERE htno = ?", sgpa, htno);
        } else {
            jdbc.update("INSERT INTO " + table + " (htno, " + column + ") VALUES (?, ?)", htno, sgpa);
        }

        // Update CGPA
        String fetchQuery = "SELECT sem_1_1, sem_1_2, sem_2_1, sem_2_2, sem_3_1, sem_3_2, sem_4_1, sem_4_2 FROM " + table + " WHERE htno = ?";
        List<Double> sgs = jdbc.query(fetchQuery, rs -> {
            List<Double> vals = new ArrayList<>();
            while (rs.next()) for (int i = 1; i <= 8; i++) {
                double val = rs.getDouble(i);
                if (!rs.wasNull()) vals.add(val);
            }
            return vals;
        }, htno);

        if (sgs != null && !sgs.isEmpty()) {
            double cgpa = sgs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            jdbc.update("UPDATE " + table + " SET cgpa = ? WHERE htno = ?", cgpa, htno);
        }
    }

    public double calculateSGPAFromList(List<Subject> subjects) {
        double totalPoints = 0, totalCredits = 0;
        for (Subject subject : subjects) {
            double gp = convertGradeToPoint(subject.getGrade());
            if (gp >= 0 && subject.getCredit() != null && subject.getCredit() > 0) {
                totalPoints += gp * subject.getCredit();
                totalCredits += subject.getCredit();
            }
        }
        return totalCredits == 0 ? 0.0 : BigDecimal.valueOf(totalPoints / totalCredits).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double convertGradeToPoint(String grade) {
        return switch (grade.trim().toUpperCase()) {
            case "A+" -> 10.0; case "A" -> 9.0; case "B" -> 8.0;
            case "C" -> 7.0; case "D" -> 6.0; case "E" -> 5.0;
            case "F" -> 0.0; default -> -1;
        };
    }

    /**
     * Extract college name from JdbcTemplate or use a default value
     */
    private String extractCollegeFromJdbc(JdbcTemplate jdbc) {
        try {
            // Try to get the college from the current context
            String college = dynamicDbService.getCurrentCollege();
            if (college != null && !college.isEmpty()) {
                return college;
            }

            // If that fails, try to extract from the JDBC URL
            javax.sql.DataSource dataSource = jdbc.getDataSource();
            if (dataSource != null) {
                java.sql.Connection connection = dataSource.getConnection();
                if (connection != null) {
                    try {
                        String url = connection.getMetaData().getURL();
                        // Get the first college from the database
                        List<String> colleges = dynamicDbService.getAllDatabases();
                        if (!colleges.isEmpty()) {
                            return colleges.get(0);
                        }
                    } finally {
                        connection.close();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting college from JDBC: {}", e.getMessage());
        }

        // Default to 'jntuh' if we can't determine the college
        return "jntuh";
    }
}
