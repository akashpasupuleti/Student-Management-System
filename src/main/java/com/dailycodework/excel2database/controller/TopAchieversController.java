//package com.dailycodework.excel2database.controller;
//
//import com.dailycodework.excel2database.domain.Student;
//import com.dailycodework.excel2database.service.StudentService;
//import org.springframework.web.bind.annotation.*;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/api")
//@CrossOrigin(origins = "*")
//public class TopAchieversController {
//
//    private final StudentService studentService;
//
//    public TopAchieversController(StudentService studentService) {
//        this.studentService = studentService;
//    }
//
//    @GetMapping("/top-achievers")
//    public Map<String, Object> getTopAchievers(
//            @RequestParam String department,
//            @RequestParam String semester) {
//
//        List<Student> allStudents = studentService.findByDepartmentAndSemester(department, semester);
//
//        List<Student> top3 = allStudents.stream()
//                .sorted(Comparator.comparingDouble(Student::getSgpa).reversed())
//                .limit(3)
//                .collect(Collectors.toList());
//
//        Student overallTopper = allStudents.stream()
//                .max(Comparator.comparingDouble(Student::getCgpa))
//                .orElse(null);
//
//        Map<String, Object> res = new HashMap<>();
//        res.put("top3", top3);
//        res.put("overallTopper", overallTopper);
//        return res;
//    }
//}
