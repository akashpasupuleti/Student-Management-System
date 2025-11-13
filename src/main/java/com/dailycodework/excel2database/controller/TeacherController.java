package com.dailycodework.excel2database.controller;

import com.dailycodework.excel2database.domain.Teacher;
import com.dailycodework.excel2database.service.EmailService;
import com.dailycodework.excel2database.service.DirectEmailService;
import com.dailycodework.excel2database.service.TeacherService;
import com.dailycodework.excel2database.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;
    private final EmailService emailService;
    private final DirectEmailService directEmailService;
    private final OtpUtil otpUtil;

    @GetMapping("/teacher/signup")
    public String showTeacherSignup() {
        return "teacher-signup";
    }

    @PostMapping("/teacher/signup")
    public String handleTeacherSignup(@ModelAttribute Teacher teacher,
                                      @RequestParam String college,
                                      @RequestParam(required = false) String otp,
                                      Model model) {

        String department = teacher.getDepartment();
        boolean isFaculty = teacher.getRole().equalsIgnoreCase("Faculty");

        if (isFaculty) {
            String hodEmail = teacherService.findHodEmail(college, department);
            if (hodEmail == null) {
                model.addAttribute("error", "No HOD found for this department.");
                return "teacher-signup";
            }

            String expectedOtp = otpUtil.getOtpForEmail(hodEmail);
            if (otp == null || !otp.equals(expectedOtp)) {
                model.addAttribute("error", "Invalid OTP or OTP required.");
                return "teacher-signup";
            }
        }

        boolean success = teacherService.registerTeacher(teacher, college);
        if (success) {
            model.addAttribute("message", "✅ Teacher registered successfully.");
            return "teacher-login";
        } else {
            model.addAttribute("error", "Teacher already exists or HOD already registered.");
            return "teacher-signup";
        }
    }

    @PostMapping("/teacher/send-otp")
    @ResponseBody
    public String sendOtpToHod(@RequestParam String college,
                               @RequestParam String department) {
        String hodEmail = teacherService.findHodEmail(college, department);
        if (hodEmail == null) {
            return "❌ No HOD found for " + department;
        }

        String otp = otpUtil.generateOtp(hodEmail);

        // Try both email services for better reliability
        boolean emailSent = false;
        try {
            emailSent = emailService.sendOtpToEmail(hodEmail, otp);
        } catch (Exception e) {
            // If Spring's EmailService fails, try the direct implementation
            emailSent = directEmailService.sendOtpToEmail(hodEmail, otp);
        }

        if (emailSent) {
            return "📩 OTP sent to HOD's email: " + hodEmail;
        } else {
            return "❌ Failed to send OTP. Please try again later.";
        }
    }

    @GetMapping("/teacher/login")
    public String showTeacherLogin() {
        return "teacher-login";
    }

    @PostMapping("/teacher/login")
    public String handleTeacherLogin(@RequestParam String firstName,
                                     @RequestParam String password,
                                     Model model) {

        String[] result = teacherService.findCollegeAndDeptByFirstName(firstName);
        if (result == null) {
            model.addAttribute("error", "❌ User not found.");
            return "teacher-login";
        }

        String college = result[0];
        String dept = result[1];

        boolean valid = teacherService.validateTeacher(firstName, password, college, dept);
        if (!valid) {
            model.addAttribute("error", "Invalid credentials.");
            return "teacher-login";
        }

        model.addAttribute("firstName", firstName);
        return "teacher-dashboard";
    }

    @GetMapping("/teacher-dashboard")
    public String showTeacherDashboard(Model model) {
        // If user is not in session, redirect to login
        // For now, we'll just return the dashboard view
        return "teacher-dashboard";
    }

    // ✅ Used for AJAX fetch from login page when "Forgot Password?" is clicked
    @GetMapping("/teacher/send-reset-link")
    @ResponseBody
    public String sendTeacherResetLink(@RequestParam String username) {
        String email = teacherService.getEmailByUsername(username);
        if (email == null) {
            return "❌ No teacher found with that username.";
        }

        String token = UUID.randomUUID().toString();
        teacherService.storeResetToken(email, token);

        // Try DirectEmailService first (same as teacher OTP)
        boolean emailSent = false;
        try {
            emailSent = directEmailService.sendTeacherPasswordResetEmail(email, token);
        } catch (Exception e) {
            // If DirectEmailService fails, try Spring's EmailService as backup
            try {
                emailSent = emailService.sendTeacherPasswordResetEmail(email, token);
            } catch (Exception ex) {
                // Both email services failed
                emailSent = false;
            }
        }

        if (emailSent) {
            return "📩 Reset link sent to: " + email;
        } else {
            return "❌ Failed to send email. Please try again later or contact support.";
        }
    }

    @GetMapping("/teacher/reset-password")
    public String showResetForm(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "teacher-reset-password";
    }

    @PostMapping("/teacher/reset-password")
    public String handleReset(@RequestParam String token,
                              @RequestParam String newPassword,
                              @RequestParam String confirmPassword,
                              Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("token", token);
            return "teacher-reset-password";
        }

        boolean updated = teacherService.updatePasswordByToken(token, newPassword);
        if (updated) {
            model.addAttribute("message", "✅ Password successfully updated.");
            return "teacher-login";
        } else {
            model.addAttribute("error", "Invalid or expired token.");
            return "teacher-reset-password";
        }
    }
}
