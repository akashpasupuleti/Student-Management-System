package com.dailycodework.excel2database.service;

import com.dailycodework.excel2database.domain.Subject;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final DynamicDatabaseService dynamicDbService;

    public void saveSubjectsToDynamicSemesterTable(List<Subject> subjects, String dept, String semester, String college) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
        String baseTableName = "results_" + dept.toLowerCase() + "_" + semester.replace("-", "_");
        String tableName = college.toLowerCase() + "_" + baseTableName;
        String table = dynamicDbService.sanitizeTableName(tableName);

        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " (" +
                "sno INT, htno VARCHAR(255), subcode VARCHAR(255), subname VARCHAR(255), " +
                "internals INT, grade VARCHAR(10), credit FLOAT)");

        for (Subject subject : subjects) {
            String check = "SELECT grade FROM " + table + " WHERE htno = ? AND subcode = ?";
            List<String> grades = jdbc.queryForList(check, String.class, subject.getHtno(), subject.getSubcode());

            if (!grades.isEmpty()) {
                double old = convertGradeToPoint(grades.get(0));
                double now = convertGradeToPoint(subject.getGrade());
                if (now > old) {
                    jdbc.update("UPDATE " + table + " SET subname=?, internals=?, grade=?, credit=? WHERE htno=? AND subcode=?",
                            subject.getSubname(), subject.getInternals(), subject.getGrade(), subject.getCredit(),
                            subject.getHtno(), subject.getSubcode());
                }
            } else {
                jdbc.update("INSERT INTO " + table + " (sno, htno, subcode, subname, internals, grade, credit) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        subject.getSno(), subject.getHtno(), subject.getSubcode(), subject.getSubname(),
                        subject.getInternals(), subject.getGrade(), subject.getCredit());
            }
        }
    }

    public List<Subject> getAllSubjectsFromDynamicTable(String dept, String semester, String college) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
        String baseTableName = "results_" + dept.toLowerCase() + "_" + semester.replace("-", "_");
        String tableName = college.toLowerCase() + "_" + baseTableName;
        String table = dynamicDbService.sanitizeTableName(tableName);

        return jdbc.query("SELECT * FROM " + table, (rs, rowNum) -> {
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
    }

    public List<String> getExistingResultTablesForDept(String dept, String college) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
        String collegePrefix = college.toLowerCase() + "_results_" + dept.toLowerCase() + "_";
        List<String> found = new ArrayList<>();

        for (String sem : List.of("1_1", "1_2", "2_1", "2_2", "3_1", "3_2", "4_1", "4_2")) {
            String tableName = collegePrefix + sem;
            List<String> tables = jdbc.queryForList("SHOW TABLES LIKE ?", String.class, tableName);
            if (!tables.isEmpty()) found.add(sem.replace("_", "-"));
        }
        return found;
    }

    public boolean isHtnoPresentInAnySemester(String dept, String htno, String college) {
        JdbcTemplate jdbc = dynamicDbService.getJdbcTemplateForCollege(college);
        String collegePrefix = college.toLowerCase() + "_results_" + dept.toLowerCase() + "_";

        for (String sem : List.of("1_1", "1_2", "2_1", "2_2", "3_1", "3_2", "4_1", "4_2")) {
            String tableName = collegePrefix + sem;
            String table = dynamicDbService.sanitizeTableName(tableName);
            try {
                Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE htno = ?", Integer.class, htno);
                if (count != null && count > 0) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private double convertGradeToPoint(String grade) {
        return switch (grade.trim().toUpperCase()) {
            case "A+" -> 10.0; case "A" -> 9.0; case "B" -> 8.0;
            case "C" -> 7.0; case "D" -> 6.0; case "E" -> 5.0;
            case "F" -> 0.0; default -> -1;
        };
    }
}
