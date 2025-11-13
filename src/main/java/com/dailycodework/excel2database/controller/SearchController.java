package com.dailycodework.excel2database.controller;

import com.dailycodework.excel2database.domain.Subject;
import com.dailycodework.excel2database.service.DynamicDatabaseService;
import com.dailycodework.excel2database.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final DynamicDatabaseService dbService;
    private final StudentService studentService;

    @GetMapping("/search-students")
    public String showSearchPage(Model model) {
        try {
            // Add necessary model attributes
            // Get a list of available colleges from table prefixes
            List<String> colleges = dbService.getAllDatabases();

            model.addAttribute("colleges", colleges);
            return "search-students";
        } catch (Exception e) {
            log.error("Error loading search page: {}", e.getMessage(), e);
            model.addAttribute("error", "An error occurred while loading the search page. Please try again.");
            return "error";
        }
    }

    @GetMapping("/api/search/subjects")
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

    @GetMapping("/api/search/search-by-roll")
    @ResponseBody
    public List<Subject> searchByRollNumber(@RequestParam String htno,
                                           @RequestParam String dept,
                                           @RequestParam String semester) {
        // Detect college for the roll number
        String college = studentService.detectCollegeForHtno(htno);
        if (college == null) {
            return List.of();
        }

        // Get results for the student
        String baseTableName = "results_" + dept.toLowerCase() + "_" + semester.replace("-", "_");
        // The sanitization is handled inside getSubjectsFromDynamicTableByHtno
        return dbService.getSubjectsFromDynamicTableByHtno(college, baseTableName, htno);
    }

    @GetMapping("/api/search/search-by-subject")
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

    @GetMapping("/api/search/subject-suggestions")
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
