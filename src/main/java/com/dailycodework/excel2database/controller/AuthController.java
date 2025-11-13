package com.dailycodework.excel2database.controller;

import com.dailycodework.excel2database.service.EmailService;
import com.dailycodework.excel2database.service.DirectEmailService;
import com.dailycodework.excel2database.service.DynamicDatabaseService;
import com.dailycodework.excel2database.service.SimpleEmailSender;
import com.dailycodework.excel2database.service.OutlookEmailSender;
import com.dailycodework.excel2database.service.MailgunEmailSender;
import com.dailycodework.excel2database.service.LocalEmailSender;
import com.dailycodework.excel2database.service.StudentService;
import com.dailycodework.excel2database.service.SubjectService;
// Student import removed to avoid compilation issues
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final StudentService studentService;
    private final SubjectService subjectService;
    private final DynamicDatabaseService dynamicDatabaseService;
    private final EmailService emailService;
    private final DirectEmailService directEmailService;
    private final SimpleEmailSender simpleEmailSender;
    private final OutlookEmailSender outlookEmailSender;
    private final MailgunEmailSender mailgunEmailSender;
    private final LocalEmailSender localEmailSender;

    @GetMapping("/signup")
    public String showSignupForm() {
        return "signup";
    }

    @PostMapping("/signup")
    public String handleSignup(@RequestParam("firstName") String firstName,
                               @RequestParam("lastName") String lastName,
                               @RequestParam("htno") String htno,
                               @RequestParam("email") String email,
                               @RequestParam("password") String password,
                               @RequestParam("confirmPassword") String confirmPassword,
                               @RequestParam("college") String college,
                               Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "signup";
        }

        boolean created = studentService.registerStudent(firstName, lastName, htno, email, password, college);
        model.addAttribute(created ? "message" : "error",
                created ? "✅ Registration successful! Please login." : "HTNO already exists.");
        return created ? "login" : "signup";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String htno,
                              @RequestParam String password,
                              Model model) {
        // Wrap the entire method in a try-catch to prevent internal server errors
        try {
            // Get the college from the student's HTNO
            try {
            String college = studentService.detectCollegeForHtno(htno);
            log.info("Detected college for HTNO {}: {}", htno, college);
            // Even if college is null, we'll continue and let validateStudent handle it
            // The validateStudent method will try to detect the college again if needed

            if (studentService.validateStudent(htno, password, college)) {
                String dept = findDeptForHtno(htno, college);
                if (dept == null) {
                    model.addAttribute("error", "❌ Unable to determine department.");
                    return "login";
                }

                // Add student info to model
                model.addAttribute("htno", htno);
                model.addAttribute("dept", dept);
                model.addAttribute("college", college);

                // Skip getting student details for now to avoid compilation issues
                // We'll add basic student info directly
                model.addAttribute("studentName", "Student");
                model.addAttribute("studentEmail", "");

                // Get all available tables for the department
                List<String> allTables = subjectService.getExistingResultTablesForDept(dept, college);

                // Get tables that have results for this student
                List<String> availableSemesters = dynamicDatabaseService.getAvailableSemesters(college, dept, htno);

                // Add both to the model
                model.addAttribute("existingTables", allTables);
                model.addAttribute("availableSemesters", availableSemesters);

                // Default to first semester with results, or first semester if none have results
                String selectedSemester = "1-1";
                if (!availableSemesters.isEmpty()) {
                    selectedSemester = availableSemesters.get(0);
                }
                model.addAttribute("selectedSemester", selectedSemester);
                return "student-dashboard";
            }

            model.addAttribute("error", "❌ Invalid Roll Number or Password.");
            return "login";
            } catch (Exception e) {
                log.error("❌ Error during student login (inner): {}", e.getMessage(), e);
                model.addAttribute("error", "❌ An error occurred. Please check your Roll Number and try again.");
                return "login";
            }
        } catch (Throwable t) {
            // Catch any possible error, including compilation errors
            log.error("❌ Critical error during student login: {}", t.getMessage(), t);
            model.addAttribute("error", "❌ A system error occurred. Please try again later or contact support.");
            return "login";
        }
    }

    private String findDeptForHtno(String htno, String college) {
        // Check all common departments
        for (String dept : List.of("CSE", "CSD", "CSM", "ECE", "EEE", "MECH", "IT", "CIVIL", "CS")) {
            if (subjectService.isHtnoPresentInAnySemester(dept, htno, college)) {
                return dept;
            }
        }

        // If no department found but student exists, default to CSE
        // This ensures students can log in even if they don't have results yet
        return "CSE";
    }

    @GetMapping("/forgot-password")
    @ResponseBody
    public String forgotPassword(@RequestParam String htno, @RequestParam(required = false) Boolean test) {
        log.info("🔍 Processing forgot password request for HTNO: {}, test mode: {}", htno, test != null && test);

        try {
            // Validate HTNO format
            if (htno == null || htno.trim().isEmpty()) {
                log.warn("Empty HTNO provided");
                return "❌ Please provide a valid Roll Number.";
            }

            // Detect college for HTNO
            String college = studentService.detectCollegeForHtno(htno);
            if (college == null) {
                log.warn("❌ No college found for HTNO: {}", htno);
                return "❌ Student not found with this Roll Number.";
            }
            log.info("Detected college for HTNO {}: {}", htno, college);

            // Get email for HTNO
            String email = studentService.getEmailByHtno(htno);
            log.info("📧 Email found for HTNO {}: {}", htno, email != null ? email : "null");

            if (email == null || email.trim().isEmpty()) {
                log.warn("❌ No email found for HTNO: {}", htno);
                return "❌ No email associated with this Roll Number. Please contact your administrator.";
            }

            // Generate a secure token
            String token = UUID.randomUUID().toString();
            log.info("🔑 Generated reset token for {}: {}", htno, token);

            // Store the token in the database
            boolean tokenStored = studentService.storeResetTokenByHtnoAndEmail(htno, email, token);
            log.info("💾 Token stored successfully: {}", tokenStored);

            if (!tokenStored) {
                log.error("❌ Failed to store reset token for HTNO: {} and email: {}", htno, email);
                return "❌ Failed to process your request. Please try again or contact support.";
            }

            // Create the reset link
            String resetLink = "http://localhost:8081/reset-password?token=" + token;

            // If this is a test request, just return the link directly
            if (test != null && test) {
                log.info("Test mode: Returning direct reset link");
                return resetLink;
            }

            // Use DirectEmailService as the primary method (same as teacher OTP)
            boolean emailSent = false;
            try {
                log.info("Trying DirectEmailService as primary method...");
                emailSent = directEmailService.sendPasswordResetEmail(email, token);
                log.info("DirectEmailService result: {}", emailSent ? "success" : "failed");
            } catch (Exception e) {
                log.error("DirectEmailService error: {}", e.getMessage(), e);
            }

            if (emailSent) {
                log.info("✅ Successfully sent password reset email to: {}", email);

                // Always log the reset link for debugging
                log.info("\n\n==================================================\n" +
                         "RESET LINK GENERATED (EMAIL SENT SUCCESSFULLY)\n" +
                         "--------------------------------------------------\n" +
                         "HTNO:  {}\n" +
                         "Email: {}\n" +
                         "Token: {}\n" +
                         "Link:  {}\n" +
                         "==================================================\n",
                         htno, email, token, resetLink);

                return "📩 Reset link sent to your registered email.";
            }

            // If DirectEmailService fails, try Spring EmailService as a backup
            try {
                log.info("Trying Spring EmailService as backup...");
                emailSent = emailService.sendPasswordResetEmail(email, token);
                if (emailSent) {
                    log.info("✅ Successfully sent password reset email using Spring EmailService");
                    return "📩 Reset link sent to your registered email.";
                }
            } catch (Exception e) {
                log.error("Spring EmailService error: {}", e.getMessage());
            }

            // If all email services failed, use LocalEmailSender as a last resort
            try {
                log.info("Trying LocalEmailSender as last resort...");
                emailSent = localEmailSender.sendPasswordResetEmail(email, token);
                if (emailSent) {
                    log.info("✅ Successfully saved password reset email to file");

                    // Create a special file with just the reset link for easy access
                    try {
                        File desktopDir = new File(System.getProperty("user.home"), "Desktop");
                        File emailsOnDesktop = new File(desktopDir, "StudentPortalEmails");
                        if (!emailsOnDesktop.exists()) {
                            emailsOnDesktop.mkdirs();
                        }

                        // Sanitize the email address for use in a filename
                        String safeEmail = email.replace("@", "_at_")
                                           .replace(".", "_")
                                           .replace(" ", "_")
                                           .replace("/", "_")
                                           .replace("\\", "_");

                        File resetLinkFile = new File(emailsOnDesktop, "RESET_LINK_" + safeEmail + ".txt");
                        try (FileWriter linkWriter = new FileWriter(resetLinkFile)) {
                            linkWriter.write("Reset link for " + email + ":\n\n" + resetLink);
                            linkWriter.flush();
                            log.info("Reset link saved to desktop: {}", resetLinkFile.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        log.error("Failed to save reset link to desktop: {}", e.getMessage());
                    }

                    return "📩 Reset link sent to your registered email.";
                }
            } catch (Exception e) {
                log.error("LocalEmailSender error: {}", e.getMessage());
            }

            // If all email services failed, provide the reset link directly
            log.error("\n\n==================================================\n" +
                     "ALL EMAIL SERVICES FAILED\n" +
                     "--------------------------------------------------\n" +
                     "HTNO:  {}\n" +
                     "Email: {}\n" +
                     "Token: {}\n" +
                     "Link:  {}\n" +
                     "==================================================\n",
                     htno, email, token, resetLink);

            // Create a special file with just the reset link for easy access
            try {
                File desktopDir = new File(System.getProperty("user.home"), "Desktop");
                File emailsOnDesktop = new File(desktopDir, "StudentPortalEmails");
                if (!emailsOnDesktop.exists()) {
                    emailsOnDesktop.mkdirs();
                }

                // Sanitize the email address for use in a filename
                String safeEmail = email.replace("@", "_at_")
                               .replace(".", "_")
                               .replace(" ", "_")
                               .replace("/", "_")
                               .replace("\\", "_");

                File resetLinkFile = new File(emailsOnDesktop, "RESET_LINK_" + safeEmail + ".txt");
                try (FileWriter linkWriter = new FileWriter(resetLinkFile)) {
                    linkWriter.write("Reset link for " + email + ":\n\n" + resetLink);
                    linkWriter.flush();
                    log.info("Reset link saved to desktop: {}", resetLinkFile.getAbsolutePath());
                }
            } catch (Exception e) {
                log.error("Failed to save reset link to desktop: {}", e.getMessage());
            }

            return "📩 Email services are currently unavailable. Please use this direct link to reset your password:\n\n" + resetLink;
        } catch (Exception e) {
            log.error("❌ Unexpected error in forgot password: {}", e.getMessage(), e);
            return "❌ An unexpected error occurred. Please try again later or contact support.";
        }
    }



    /**
     * Get the reset link for a given HTNO
     */
    @GetMapping("/get-reset-link")
    @ResponseBody
    public String getResetLink(@RequestParam String htno) {
        log.info("Getting reset link for HTNO: {}", htno);

        try {
            // Detect college for HTNO
            String college = studentService.detectCollegeForHtno(htno);
            if (college == null) {
                return "College not found for HTNO: " + htno;
            }

            // Get email for HTNO
            String email = studentService.getEmailByHtno(htno);
            if (email == null) {
                return "Email not found for HTNO: " + htno;
            }

            // Get the most recent token for this HTNO
            String token = studentService.getResetTokenByHtno(htno);
            if (token == null) {
                // Generate a new token if none exists
                token = UUID.randomUUID().toString();
                boolean tokenStored = studentService.storeResetTokenByHtnoAndEmail(htno, email, token);
                if (!tokenStored) {
                    return "Failed to store token for HTNO: " + htno;
                }
            }

            // Create reset link
            String resetLink = "http://localhost:8081/reset-password?token=" + token;

            // Save the reset link to the desktop
            try {
                File desktopDir = new File(System.getProperty("user.home"), "Desktop");
                File emailsOnDesktop = new File(desktopDir, "StudentPortalEmails");
                if (!emailsOnDesktop.exists()) {
                    emailsOnDesktop.mkdirs();
                }

                // Sanitize the email address for use in a filename
                String safeEmail = email.replace("@", "_at_")
                                   .replace(".", "_")
                                   .replace(" ", "_")
                                   .replace("/", "_")
                                   .replace("\\", "_");

                File resetLinkFile = new File(emailsOnDesktop, "RESET_LINK_" + safeEmail + ".txt");
                try (FileWriter linkWriter = new FileWriter(resetLinkFile)) {
                    linkWriter.write("Reset link for " + email + ":\n\n" + resetLink);
                    linkWriter.flush();
                    log.info("Reset link saved to desktop: {}", resetLinkFile.getAbsolutePath());
                }
            } catch (Exception e) {
                log.error("Failed to save reset link to desktop: {}", e.getMessage());
            }

            return "Reset link for " + htno + ":\n\n" + resetLink + "\n\n(A copy of this link has also been saved to your Desktop in the StudentPortalEmails folder)";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }


    @PostMapping("/forgot-password")
    @ResponseBody
    public String sendResetLink(@RequestParam String htno,
                                @RequestParam String email) {
        String token = UUID.randomUUID().toString();
        boolean stored = studentService.storeResetTokenByHtnoAndEmail(htno, email, token);
        if (stored) {
            // Try all available email services for better reliability
            boolean emailSent = false;
            Exception lastException = null;

            log.info("Attempting to send password reset email to {} using multiple methods", email);

            // Try Spring EmailService
            try {
                log.info("Trying Spring EmailService first...");
                emailSent = emailService.sendPasswordResetEmail(email, token);
                log.info("Spring EmailService result: {}", emailSent ? "success" : "failed");
                if (emailSent) {
                    log.info("Successfully sent password reset email using Spring EmailService");
                }
            } catch (Exception e) {
                log.error("Spring EmailService error: {}", e.getMessage());
                lastException = e;

                // Try DirectEmailService
                try {
                    log.info("Trying DirectEmailService...");
                    emailSent = directEmailService.sendPasswordResetEmail(email, token);
                    log.info("DirectEmailService result: {}", emailSent ? "success" : "failed");
                    if (emailSent) {
                        log.info("Successfully sent password reset email using DirectEmailService");
                    }
                } catch (Exception e2) {
                    log.error("DirectEmailService error: {}", e2.getMessage());
                    lastException = e2;

                    // Try SimpleEmailSender
                    try {
                        log.info("Trying SimpleEmailSender...");
                        emailSent = simpleEmailSender.sendPasswordResetEmail(email, token);
                        log.info("SimpleEmailSender result: {}", emailSent ? "success" : "failed");
                        if (emailSent) {
                            log.info("Successfully sent password reset email using SimpleEmailSender");
                        }
                    } catch (Exception e3) {
                        log.error("SimpleEmailSender error: {}", e3.getMessage());
                        lastException = e3;

                        // Try OutlookEmailSender
                        try {
                            log.info("Trying OutlookEmailSender...");
                            emailSent = outlookEmailSender.sendPasswordResetEmail(email, token);
                            log.info("OutlookEmailSender result: {}", emailSent ? "success" : "failed");
                            if (emailSent) {
                                log.info("Successfully sent password reset email using OutlookEmailSender");
                            }
                        } catch (Exception e4) {
                            log.error("OutlookEmailSender error: {}", e4.getMessage());
                            lastException = e4;

                            // Try MailgunEmailSender
                            try {
                                log.info("Trying MailgunEmailSender...");
                                emailSent = mailgunEmailSender.sendPasswordResetEmail(email, token);
                                log.info("MailgunEmailSender result: {}", emailSent ? "success" : "failed");
                                if (emailSent) {
                                    log.info("Successfully sent password reset email using MailgunEmailSender");
                                }
                            } catch (Exception e5) {
                                log.error("MailgunEmailSender error: {}", e5.getMessage());
                                lastException = e5;

                                // Try LocalEmailSender as last resort
                                try {
                                    log.info("Trying LocalEmailSender as last resort...");
                                    emailSent = localEmailSender.sendPasswordResetEmail(email, token);
                                    log.info("LocalEmailSender result: {}", emailSent ? "success" : "failed");
                                    if (emailSent) {
                                        log.info("Successfully sent password reset email using LocalEmailSender");
                                    }
                                } catch (Exception e6) {
                                    log.error("LocalEmailSender error: {}", e6.getMessage());
                                    lastException = e6;
                                }
                            }
                        }
                    }
                }
            }
            if (emailSent) {
                return "📩 Reset link sent to your email.";
            } else {
                String errorMessage = lastException != null ?
                    lastException.getMessage() : "Unknown error";
                log.error("All email methods failed. Last error: {}", errorMessage);
                return "❌ Failed to send email. Please try again later or contact support.\nError: " + errorMessage;
            }
        } else {
            return "❌ HTNO/Email combination not found.";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPage(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }







    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("token") String token,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("token", token);
            return "reset-password";
        }

        boolean success = studentService.updatePassword(token, newPassword);
        if (success) {
            model.addAttribute("message", "✅ Password updated.");
            return "login";
        } else {
            model.addAttribute("error", "Invalid or expired link.");
            return "reset-password";
        }
    }
}
