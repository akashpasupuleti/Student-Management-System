package com.dailycodework.excel2database.controller;

import com.dailycodework.excel2database.domain.Subject;
import com.dailycodework.excel2database.service.DynamicDatabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class EnhancedSearchController {

    private final DynamicDatabaseService dbService;

    @GetMapping("/enhanced-search")
    public String showSearchPage(Model model) {
        try {
            log.info("Loading enhanced search page");

            // Get a list of available colleges from table prefixes
            List<String> colleges = dbService.getAllDatabases();
            model.addAttribute("colleges", colleges);

            // Add departments list
            List<String> departments = Arrays.asList("CS", "ECE", "EEE", "MECH", "IT", "CIVIL");
            model.addAttribute("departments", departments);

            // Add sub-departments for CS
            Map<String, List<String>> subDepartments = new HashMap<>();
            subDepartments.put("CS", Arrays.asList("CSE", "CSD", "CSM"));
            model.addAttribute("subDepartments", subDepartments);

            // Add semesters
            List<String> semesters = Arrays.asList("1-1", "1-2", "2-1", "2-2", "3-1", "3-2", "4-1", "4-2");
            model.addAttribute("semesters", semesters);

            return "enhanced-search";
        } catch (Exception e) {
            log.error("Error loading enhanced search page: {}", e.getMessage(), e);
            model.addAttribute("error", "An error occurred while loading the search page: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/api/departments")
    @ResponseBody
    public List<String> getDepartments() {
        return Arrays.asList("CS", "ECE", "EEE", "MECH", "IT", "CIVIL");
    }

    @GetMapping("/api/sub-departments")
    @ResponseBody
    public List<String> getSubDepartments(@RequestParam String department) {
        if ("CS".equals(department)) {
            return Arrays.asList("CSE", "CSD", "CSM");
        }
        return Collections.emptyList();
    }

    @GetMapping("/api/semesters")
    @ResponseBody
    public List<String> getSemesters() {
        return Arrays.asList("1-1", "1-2", "2-1", "2-2", "3-1", "3-2", "4-1", "4-2");
    }

    @GetMapping("/api/subjects")
    @ResponseBody
    public List<String> getSubjects(@RequestParam String dept,
                                    @RequestParam String semester,
                                    @RequestParam(required = false) String college) {
        // If college is not provided, use the first available college
        if (college == null || college.isEmpty()) {
            List<String> colleges = dbService.getAllDatabases();
            if (colleges.isEmpty()) {
                return List.of();
            }
            college = colleges.get(0);
        }

        try {
            // Get all subjects for the department and semester
            String cleanSemester = semester.replace("-", "_");
            List<Subject> subjects = dbService.getAllSubjectsFromDynamicTable(college, dept, cleanSemester);

            // Extract unique subject names
            return subjects.stream()
                    .map(Subject::getSubname)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching subjects: {}", e.getMessage());
            return List.of();
        }
    }

    @GetMapping("/api/search-by-roll")
    @ResponseBody
    public Map<String, Object> searchByRollNumber(@RequestParam String htno,
                                           @RequestParam String dept,
                                           @RequestParam String semester) {
        Map<String, Object> response = new HashMap<>();

        // Get base table name for the semester
        String baseTableName = "results_" + dept.toLowerCase() + "_" + semester.replace("-", "_");
        log.info("Searching for roll number: {} in semester: {} (base table: {})", htno, semester, baseTableName);

        // Get all colleges from table prefixes
        List<String> colleges = dbService.getAllDatabases();
        JdbcTemplate jdbc = dbService.getJdbcTemplateForCollege(null);

        // Try all colleges
        for (String college : colleges) {

            try {
                // Create the full table name with college prefix
                String tableName = college.toLowerCase() + "_" + baseTableName;
                String sanitizedTable = dbService.sanitizeTableName(tableName);

                // Check if the table exists
                List<String> tables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, tableName);
                if (tables.isEmpty()) {
                    log.info("Table {} does not exist in database, skipping", tableName);
                    continue;
                }

                // Table exists, check if student exists in this table
                Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + sanitizedTable + " WHERE htno = ?", Integer.class, htno);

                if (count == null || count == 0) {
                    log.info("No records found for htno: {} in table: {} for college: {}", htno, tableName, college);
                    continue;
                }

                // Student exists in this table, get their subjects
                log.info("Found {} records for htno: {} in table: {} for college: {}", count, htno, tableName, college);

                List<Subject> subjects = jdbc.query(
                    "SELECT * FROM " + sanitizedTable + " WHERE htno = ?",
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

                if (!subjects.isEmpty()) {
                    response.put("subjects", subjects);

                    // STEP 1: Calculate SGPA from current semester subjects
                    calculateAndAddSgpa(subjects, response);
                    log.info("Calculated SGPA for current semester: {}", response.get("sgpa"));

                    // STEP 2: Calculate CGPA from all semesters
                    calculateAndAddCgpa(jdbc, htno, dept, response);
                    log.info("Calculated CGPA from all semesters: {}", response.get("cgpa"));

                    // STEP 3: Try to get official SGPA and CGPA from database (if available)
                    try {
                        String baseGradesTable = "grades_" + dept.toLowerCase();
                        String gradesTable = college.toLowerCase() + "_" + baseGradesTable;
                        String sanitizedGradesTable = dbService.sanitizeTableName(gradesTable);
                        String semesterColumn = "sem_" + semester.replace("-", "_");

                        // Check if grades table exists
                        List<String> gradesTables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, gradesTable);
                        if (!gradesTables.isEmpty()) {
                            try {
                                String query = "SELECT " + semesterColumn + ", cgpa FROM " + sanitizedGradesTable + " WHERE htno = ?";
                                Map<String, Object> gradesResult = jdbc.queryForMap(query, htno);

                                Double sgpa = (Double) gradesResult.get(semesterColumn);
                                Double cgpa = (Double) gradesResult.get("cgpa");

                                // Override calculated values with database values if available
                                if (sgpa != null) {
                                    response.put("sgpa", sgpa);
                                    log.info("Using database SGPA: {}", sgpa);
                                }

                                if (cgpa != null) {
                                    response.put("cgpa", cgpa);
                                    log.info("Using database CGPA: {}", cgpa);
                                }
                            } catch (Exception e) {
                                log.warn("Error getting SGPA/CGPA from database: {}", e.getMessage());
                                // We already calculated SGPA and CGPA above, so no need to recalculate
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error accessing SGPA/CGPA tables: {}", e.getMessage());
                        // We already calculated SGPA and CGPA above, so no need to recalculate
                    }

                    // Final check to ensure both SGPA and CGPA are in the response
                    if (!response.containsKey("sgpa")) {
                        response.put("sgpa", 0.0);
                        log.warn("SGPA still missing, setting default to 0.0 for htno: {}", htno);
                    }
                    if (!response.containsKey("cgpa")) {
                        response.put("cgpa", 0.0);
                        log.warn("CGPA still missing, setting default to 0.0 for htno: {}", htno);
                    }

                    // Log the final values for debugging
                    log.info("Final SGPA: {}, CGPA: {} for htno: {}",
                            response.get("sgpa"), response.get("cgpa"), htno);

                    return response; // Return as soon as we find results
                }
            } catch (Exception e) {
                log.error("Error checking college {}: {}", college, e.getMessage());
            }
        }

        // If we get here, no results were found
        response.put("subjects", List.of());
        return response;
    }

    private double convertGradeToPoint(String grade) {
        if (grade == null) return -1;
        return switch (grade.trim().toUpperCase()) {
            case "A+" -> 10.0; case "A" -> 9.0;
            case "B" -> 8.0;
            case "C" -> 7.0;
            case "D" -> 6.0;
            case "E" -> 5.0;
            case "F" -> 0.0;
            case "MP" -> 0.0; // Malpractice counts as 0
            case "AB" -> 0.0; // Absent counts as 0
            default -> -1;
        };
    }

    private void calculateAndAddSgpa(List<Subject> subjects, Map<String, Object> response) {
        double totalPoints = 0;
        double totalCredits = 0;

        for (Subject subject : subjects) {
            double gp = convertGradeToPoint(subject.getGrade());
            if (gp >= 0 && subject.getCredit() != null && subject.getCredit() > 0) {
                totalPoints += gp * subject.getCredit();
                totalCredits += subject.getCredit();
            }
        }

        if (totalCredits > 0) {
            double sgpa = BigDecimal.valueOf(totalPoints / totalCredits)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
            response.put("sgpa", sgpa);
            log.info("Calculated SGPA on the fly: {}", sgpa);
        }
    }

    private void calculateAndAddCgpa(JdbcTemplate jdbc, String htno, String dept, Map<String, Object> response) {
        try {
            log.info("Calculating CGPA for htno: {} in department: {}", htno, dept);

            // Get all semester results for this student
            List<Subject> allSubjects = new ArrayList<>();

            // Get list of all results tables for this department across all colleges
            List<String> colleges = dbService.getAllDatabases();
            List<String> resultsTables = new ArrayList<>();

            for (String college : colleges) {
                String tablePattern = college.toLowerCase() + "_results_" + dept.toLowerCase() + "_%";
                List<String> collegeTables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, tablePattern);
                resultsTables.addAll(collegeTables);
            }

            log.info("Found {} results tables for department: {}", resultsTables.size(), dept);

            for (String table : resultsTables) {
                try {
                    String sanitizedTable = dbService.sanitizeTableName(table);
                    List<Subject> semesterSubjects = jdbc.query(
                        "SELECT * FROM " + sanitizedTable + " WHERE htno = ?",
                        (rs, rowNum) -> {
                            Subject s = new Subject();
                            s.setHtno(rs.getString("htno"));
                            s.setSubcode(rs.getString("subcode"));
                            s.setSubname(rs.getString("subname"));
                            s.setGrade(rs.getString("grade"));
                            s.setCredit(rs.getDouble("credit"));
                            return s;
                        },
                        htno
                    );
                    log.info("Found {} subjects in table: {}", semesterSubjects.size(), table);
                    allSubjects.addAll(semesterSubjects);
                } catch (Exception e) {
                    log.debug("Error fetching from table {}: {}", table, e.getMessage());
                }
            }

            if (!allSubjects.isEmpty()) {
                double totalPoints = 0;
                double totalCredits = 0;

                for (Subject subject : allSubjects) {
                    double gp = convertGradeToPoint(subject.getGrade());
                    if (gp >= 0 && subject.getCredit() != null && subject.getCredit() > 0) {
                        totalPoints += gp * subject.getCredit();
                        totalCredits += subject.getCredit();
                    }
                }

                if (totalCredits > 0) {
                    double cgpa = BigDecimal.valueOf(totalPoints / totalCredits)
                            .setScale(2, RoundingMode.HALF_UP).doubleValue();
                    response.put("cgpa", cgpa);
                    log.info("Calculated CGPA: {} for htno: {}", cgpa, htno);
                }
            } else {
                log.info("No subjects found for CGPA calculation for htno: {}", htno);
                // If we can't calculate CGPA, set a default value to ensure it's displayed
                // This is a fallback to ensure CGPA is always included in the response
                if (!response.containsKey("cgpa")) {
                    // If we have SGPA, use it as a fallback for CGPA
                    if (response.containsKey("sgpa")) {
                        response.put("cgpa", response.get("sgpa"));
                        log.info("Using SGPA as fallback for CGPA: {}", response.get("sgpa"));
                    } else {
                        // Last resort - set a default CGPA
                        response.put("cgpa", 0.0);
                        log.info("Setting default CGPA to 0.0 for htno: {}", htno);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error calculating CGPA: {}", e.getMessage(), e);
            // Even if there's an error, ensure CGPA is included in the response
            if (!response.containsKey("cgpa")) {
                // If we have SGPA, use it as a fallback for CGPA
                if (response.containsKey("sgpa")) {
                    response.put("cgpa", response.get("sgpa"));
                    log.info("Using SGPA as fallback for CGPA after error: {}", response.get("sgpa"));
                } else {
                    // Last resort - set a default CGPA
                    response.put("cgpa", 0.0);
                    log.info("Setting default CGPA to 0.0 after error for htno: {}", htno);
                }
            }
        }

        // Final check to ensure CGPA is in the response
        if (!response.containsKey("cgpa")) {
            response.put("cgpa", 0.0);
            log.warn("CGPA still missing after calculation, setting default to 0.0 for htno: {}", htno);
        }
    }

    @GetMapping("/api/search-by-subject")
    @ResponseBody
    public List<Map<String, Object>> searchBySubject(@RequestParam String subject,
                                                    @RequestParam String grade,
                                                    @RequestParam String dept,
                                                    @RequestParam String semester) {
        List<Map<String, Object>> results = new ArrayList<>();
        JdbcTemplate jdbc = dbService.getJdbcTemplateForCollege(null);

        // Get all colleges from table prefixes
        List<String> colleges = dbService.getAllDatabases();

        for (String college : colleges) {
            try {
                String baseTableName = "results_" + dept.toLowerCase() + "_" + semester.replace("-", "_");
                String tableName = college.toLowerCase() + "_" + baseTableName;
                String sanitizedTableName = dbService.sanitizeTableName(tableName);

                // Query for students with the given subject and grade
                String sql = "SELECT htno, subname, grade, internals FROM " + sanitizedTableName +
                             " WHERE subname LIKE ? AND grade = ?";

                List<Map<String, Object>> queryResults = jdbc.queryForList(sql,
                    "%" + subject + "%", grade);

                for (Map<String, Object> row : queryResults) {
                    row.put("college", college);
                    results.add(row);
                }
            } catch (Exception e) {
                log.warn("Error searching in college {}: {}", college, e.getMessage());
            }
        }

        return results;
    }

    @GetMapping("/api/subject-suggestions")
    @ResponseBody
    public List<String> getSubjectSuggestions(@RequestParam String query,
                                             @RequestParam String dept,
                                             @RequestParam String semester) {
        List<String> suggestions = new ArrayList<>();
        JdbcTemplate jdbc = dbService.getJdbcTemplateForCollege(null);

        // Get all colleges from table prefixes
        List<String> colleges = dbService.getAllDatabases();

        for (String college : colleges) {
            try {
                String baseTableName = "results_" + dept.toLowerCase() + "_" + semester.replace("-", "_");
                String tableName = college.toLowerCase() + "_" + baseTableName;
                String sanitizedTableName = dbService.sanitizeTableName(tableName);

                // Query for subject names that match the query
                String sql = "SELECT DISTINCT subname FROM " + sanitizedTableName + " WHERE subname LIKE ?";
                List<String> subjects = jdbc.queryForList(sql, String.class, "%" + query + "%");
                suggestions.addAll(subjects);
            } catch (Exception e) {
                // Ignore errors and continue with next college
            }
        }

        // Remove duplicates and return
        return suggestions.stream().distinct().collect(Collectors.toList());
    }
}
