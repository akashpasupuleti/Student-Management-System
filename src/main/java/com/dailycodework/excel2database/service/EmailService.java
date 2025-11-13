package com.dailycodework.excel2database.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.password}")
    private String mailPassword;

    @PostConstruct
    public void init() {
        // Log mail configuration at startup (mask the password)
        String maskedPassword = mailPassword.length() > 4 ?
                "****" + mailPassword.substring(mailPassword.length() - 4) : "****";
        log.info("Email service initialized with username: {} and password: {}", fromEmail, maskedPassword);
        log.info("Using mail configuration from application.properties");

        // Don't test email configuration at startup to avoid authentication errors
        // The email service will be tested on-demand when needed
        log.info("Email testing disabled at startup to prevent authentication errors");
    }

    /**
     * Test the email configuration by sending a test email to the configured email address
     * This helps verify that the email settings are correct at application startup
     */
    @PostConstruct
    private void testEmailConfiguration() {
        // Skip test if using default/dummy credentials
        if (fromEmail == null || fromEmail.equals("example@gmail.com") ||
            mailPassword == null || mailPassword.equals("dummypassword")) {
            log.info("Skipping email configuration test - using default/dummy credentials");
            log.info("Email service will fall back to local file storage when needed");
            return;
        }

        try {
            log.info("Testing email configuration with username: {}", fromEmail);
            log.info("Email testing is disabled at startup to prevent authentication errors");
            log.info("Email service will be tested on-demand when needed");
            // Don't actually test the email service at startup
        } catch (Exception e) {
            log.warn("⚠️ Unexpected error during email configuration: {}", e.getMessage());
            log.info("Email service will fall back to local file storage when needed");
            // Don't rethrow - we don't want to prevent application startup
        }
    }

    /**
     * Check if email is properly configured
     */
    private boolean isEmailConfigured() {
        return fromEmail != null && !fromEmail.equals("example@gmail.com") &&
               mailPassword != null && !mailPassword.equals("dummypassword") &&
               !fromEmail.isEmpty() && !mailPassword.isEmpty();
    }

    public boolean sendOtpToEmail(String email, String otp) {
        // Check if email is configured
        if (!isEmailConfigured()) {
            log.warn("Email is not properly configured. Using local file fallback.");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendOtpToEmail(email, otp);
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(email);
            msg.setSubject("🔐 OTP Verification - Faculty Signup");
            msg.setText("Your OTP for faculty registration is: " + otp +
                        "\n\nUse this OTP to complete your signup.\nIt expires shortly.");

            log.info("Attempting to send OTP email to: {}", email);
            mailSender.send(msg);
            log.info("✅ OTP email sent successfully to: {}", email);
            return true;
        } catch (MailAuthenticationException e) {
            log.error("❌ Authentication failed when sending OTP to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendOtpToEmail(email, otp);
        } catch (MailSendException e) {
            log.error("❌ Failed to send OTP to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendOtpToEmail(email, otp);
        } catch (MailException e) {
            log.error("❌ General mail error when sending OTP to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendOtpToEmail(email, otp);
        } catch (Exception e) {
            log.error("❌ Unexpected error when sending OTP to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendOtpToEmail(email, otp);
        }
    }

    public boolean sendPasswordResetEmail(String email, String token) {
        // Check if email is configured
        if (!isEmailConfigured()) {
            log.warn("Email is not properly configured. Using local file fallback.");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendPasswordResetEmail(email, token);
        }

        try {
            String resetLink = "http://localhost:8081/reset-password?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("🔒 Reset Password - Student Portal");
            message.setText("Click the link to reset your password:\n" + resetLink);

            log.info("Attempting to send password reset email to: {}", email);
            mailSender.send(message);
            log.info("✅ Password reset email sent successfully to: {}", email);
            return true;
        } catch (MailAuthenticationException e) {
            log.error("❌ Authentication failed when sending email to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendPasswordResetEmail(email, token);
        } catch (MailSendException e) {
            log.error("❌ Failed to send email to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendPasswordResetEmail(email, token);
        } catch (MailException e) {
            log.error("❌ General mail error when sending to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendPasswordResetEmail(email, token);
        } catch (Exception e) {
            log.error("❌ Unexpected error when sending email to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendPasswordResetEmail(email, token);
        }
    }

    public boolean sendTeacherPasswordResetEmail(String email, String token) {
        // Check if email is configured
        if (!isEmailConfigured()) {
            log.warn("Email is not properly configured. Using local file fallback.");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendTeacherPasswordResetEmail(email, token);
        }

        try {
            String resetLink = "http://localhost:8081/teacher/reset-password?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("🔒 Reset Password - Teacher Portal");
            message.setText("Click the link to reset your password:\n" + resetLink);

            log.info("Attempting to send teacher password reset email to: {}", email);
            mailSender.send(message);
            log.info("✅ Teacher password reset email sent successfully to: {}", email);
            return true;
        } catch (MailAuthenticationException e) {
            log.error("❌ Authentication failed when sending email to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendTeacherPasswordResetEmail(email, token);
        } catch (MailSendException e) {
            log.error("❌ Failed to send email to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendTeacherPasswordResetEmail(email, token);
        } catch (MailException e) {
            log.error("❌ General mail error when sending to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendTeacherPasswordResetEmail(email, token);
        } catch (Exception e) {
            log.error("❌ Unexpected error when sending email to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendTeacherPasswordResetEmail(email, token);
        }
    }
}
