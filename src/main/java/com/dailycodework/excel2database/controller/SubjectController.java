package com.dailycodework.excel2database.controller;

import com.dailycodework.excel2database.domain.Subject;
import com.dailycodework.excel2database.service.DynamicDatabaseService;
import com.dailycodework.excel2database.service.SgpaService;
import com.dailycodework.excel2database.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SubjectController {

    private final DynamicDatabaseService dbService;
    private final SgpaService sgpaService;
    private final StudentService studentService;  // ✅ Injected correctly

    /**
     * ✅ Used in student-dashboard to fetch subject data for a semester
     */
    @GetMapping("/subjects/api/view-semester")
    @ResponseBody
    public List<Subject> viewSemesterResults(@RequestParam String htno,
                                             @RequestParam String dept,
                                             @RequestParam String semester) {
        try {
            // ✅ Auto-detect college for given HTNO
            String college = studentService.detectCollegeForHtno(htno);
            if (college == null) {
                log.error("❌ College not found for HTNO: {}", htno);
                return List.of(); // Return empty list if college not found
            }

            String baseTableName = "results_" + dept.toLowerCase() + "_" + semester.replace("-", "_");
            List<Subject> subjects = dbService.getSubjectsFromDynamicTableByHtno(college, baseTableName, htno);

            // Log the results for debugging
            log.info("Retrieved {} subjects for student {} from table {}",
                    subjects != null ? subjects.size() : 0, htno, baseTableName);

            // Ensure we never return null
            return subjects != null ? subjects : List.of();
        } catch (Exception e) {
            log.error("❌ Error retrieving semester results for HTNO {}: {}", htno, e.getMessage(), e);
            return List.of(); // Return empty list on error
        }
    }

    /**
     * ✅ Trigger SGPA calculation from UI (admin side)
     */
    @GetMapping("/subjects/calculate-all-sgpa")
    public String calculateAllSGPA(@RequestParam String semester,
                                   @RequestParam String dept,
                                   @RequestParam String college,
                                   Model model) {
        try {
            List<Subject> subjects = dbService.getAllSubjectsFromDynamicTable(college, dept, semester);
            sgpaService.calculateAndStoreAllSGPAForSemester(subjects, dept, semester, college);
            model.addAttribute("message", "✅ SGPA calculated for all students in " + dept + " - " + semester);
        } catch (Exception e) {
            model.addAttribute("error", "❌ SGPA calculation failed: " + e.getMessage());
            return "error-page";
        }
        return "sgpa-results";
    }

    /**
     * API endpoint to get CGPA for a student
     */
    @GetMapping("/subjects/api/get-cgpa")
    @ResponseBody
    public java.util.Map<String, Object> getCGPA(@RequestParam String htno,
                                                @RequestParam String dept) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();

        try {
            // Auto-detect college for given HTNO
            String college = studentService.detectCollegeForHtno(htno);
            if (college == null) {
                log.error("❌ College not found for HTNO: {}", htno);
                response.put("error", "Student not found");
                return response; // Return response with error message if college not found
            }

            log.info("Getting CGPA for HTNO: {}, Dept: {}, College: {}", htno, dept, college);

            // Add available semesters to the response
            List<String> availableSemesters = dbService.getAvailableSemesters(college, dept, htno);
            response.put("availableSemesters", availableSemesters);
            log.info("Available semesters for HTNO {}: {}", htno, availableSemesters);

            // Check if student exists in students table
            boolean studentExists = dbService.studentExistsInStudentsTable(college, htno);
            response.put("studentExists", studentExists);
            log.info("Student exists in students table: {}", studentExists);

            try {
                // First try the new college_grades_dept table format
                String baseGradesTable = "grades_" + dept.toLowerCase();
                String gradesTable = college.toLowerCase() + "_" + baseGradesTable;
                String sanitizedGradesTable = dbService.sanitizeTableName(gradesTable);
                org.springframework.jdbc.core.JdbcTemplate jdbc = dbService.getJdbcTemplateForCollege(college);

                log.info("Looking for CGPA in table: {}", gradesTable);

                // Check if the grades table exists
                java.util.List<String> gradesTables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, gradesTable);
                if (!gradesTables.isEmpty()) {
                    String query = "SELECT cgpa FROM " + sanitizedGradesTable + " WHERE htno = ?";
                    log.debug("Executing query: {}", query);

                    try {
                        Double cgpa = jdbc.queryForObject(query, Double.class, htno);
                        log.info("Found CGPA for HTNO {}: {}", htno, cgpa);

                        if (cgpa != null) {
                            response.put("cgpa", cgpa);
                        }
                    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                        log.info("No CGPA found for HTNO {} in table {}", htno, gradesTable);
                    } catch (Exception e) {
                        log.warn("Error fetching CGPA for {} from {}: {}", htno, gradesTable, e.getMessage());
                    }
                } else {
                    log.info("Grades table {} does not exist", gradesTable);

                    // Try the old sgpa_results_dept table format as fallback
                    String oldSgpaTable = "sgpa_results_" + dept.toLowerCase();
                    String sanitizedOldTable = dbService.sanitizeTableName(oldSgpaTable);

                    log.info("Looking for CGPA in old table format: {}", oldSgpaTable);

                    java.util.List<String> oldTables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, oldSgpaTable);
                    if (!oldTables.isEmpty()) {
                        String query = "SELECT cgpa FROM " + sanitizedOldTable + " WHERE htno = ?";
                        log.debug("Executing query: {}", query);

                        try {
                            Double cgpa = jdbc.queryForObject(query, Double.class, htno);
                            log.info("Found CGPA for HTNO {} in old table: {}", htno, cgpa);

                            if (cgpa != null) {
                                response.put("cgpa", cgpa);
                            }
                        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                            log.info("No CGPA found for HTNO {} in old table {}", htno, oldSgpaTable);
                        } catch (Exception e) {
                            log.warn("Error fetching CGPA from old table for {}: {}", htno, e.getMessage());
                        }
                    } else {
                        log.info("Old SGPA table {} does not exist", oldSgpaTable);
                    }
                }
            } catch (Exception e) {
                log.error("Error getting CGPA: {}", e.getMessage(), e);
            }

            return response;
        } catch (Exception e) {
            log.error("❌ Error in getCGPA for HTNO {}: {}", htno, e.getMessage(), e);
            response.put("error", "An error occurred while retrieving student data");
            return response;
        }
    }
}
