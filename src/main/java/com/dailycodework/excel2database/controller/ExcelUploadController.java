package com.dailycodework.excel2database.controller;

import com.dailycodework.excel2database.domain.Subject;
import com.dailycodework.excel2database.service.ExcelUploadService;
import com.dailycodework.excel2database.service.SubjectService;
import com.dailycodework.excel2database.service.SgpaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/excel")
public class ExcelUploadController {

    private final ExcelUploadService excelUploadService;
    private final SubjectService subjectService;
    private final SgpaService sgpaService;

    @GetMapping("/upload-form")
    public String showUploadForm() {
        return "uploadForm";
    }

    @PostMapping("/upload")
    public String handleUpload(@RequestParam("file") MultipartFile file,
                               @RequestParam("mainDept") String mainDept,
                               @RequestParam(value = "subDept", required = false) String subDept,
                               @RequestParam("semester") String semester,
                               @RequestParam("type") String type,
                               @RequestParam("college") String college,
                               Model model) {

        // Add form data back to model to preserve values on error
        model.addAttribute("mainDept", mainDept);
        model.addAttribute("subDept", subDept);
        model.addAttribute("semester", semester);
        model.addAttribute("type", type);
        model.addAttribute("college", college);

        try {
            String dept = mainDept.equals("CS") ? subDept : mainDept;
            String cleanSemester = semester.replace("-", "_");

            List<Subject> subjects = excelUploadService.getSubjectsDataFromExcel(file.getInputStream());

            subjectService.saveSubjectsToDynamicSemesterTable(subjects, dept, cleanSemester, college);
            sgpaService.calculateAndStoreAllSGPAForSemester(subjects, dept, cleanSemester, college);

            model.addAttribute("message", type.equalsIgnoreCase("regular") ?
                    "✅ Uploaded Results for " + dept + " - " + semester :
                    "✅ Updated Supplementary Results for " + dept + " - " + semester);

        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            model.addAttribute("error", "❌ Upload Failed: " + e.getMessage());
        }

        return "uploadForm";
    }

    /**
     * Handle AJAX upload requests
     */
    @PostMapping("/upload-ajax")
    @ResponseBody
    public Map<String, String> handleAjaxUpload(@RequestParam("file") MultipartFile file,
                                              @RequestParam("mainDept") String mainDept,
                                              @RequestParam(value = "subDept", required = false) String subDept,
                                              @RequestParam("semester") String semester,
                                              @RequestParam("type") String type,
                                              @RequestParam("college") String college) {

        Map<String, String> response = new HashMap<>();

        try {
            String dept = mainDept.equals("CS") ? subDept : mainDept;
            String cleanSemester = semester.replace("-", "_");

            List<Subject> subjects = excelUploadService.getSubjectsDataFromExcel(file.getInputStream());

            subjectService.saveSubjectsToDynamicSemesterTable(subjects, dept, cleanSemester, college);
            sgpaService.calculateAndStoreAllSGPAForSemester(subjects, dept, cleanSemester, college);

            String message = type.equalsIgnoreCase("regular") ?
                    "✅ Uploaded Results for " + dept + " - " + semester :
                    "✅ Updated Supplementary Results for " + dept + " - " + semester;

            response.put("status", "success");
            response.put("message", message);

        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "❌ Upload Failed: " + e.getMessage());
        }

        return response;
    }
}
