package com.dailycodework.excel2database.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Email sender that saves emails to a local file for testing
 * This is useful when you don't have access to an SMTP server
 */
@Service
@Slf4j
public class LocalEmailSender {

    private final String FROM_EMAIL = "noreply@localhost";
    private File emailDir;

    public LocalEmailSender() {
        // Try multiple locations in order of preference
        File[] possibleDirs = {
            // 1. User's home directory
            new File(System.getProperty("user.home"), "student_portal_emails"),
            // 2. Current working directory
            new File(System.getProperty("user.dir"), "emails"),
            // 3. Temp directory
            new File(System.getProperty("java.io.tmpdir"), "student_portal_emails")
        };

        boolean foundWritableDir = false;

        // Try each directory until we find one that works
        for (File dir : possibleDirs) {
            try {
                log.info("Trying to use directory: {}", dir.getAbsolutePath());

                // Create directory if it doesn't exist
                if (!dir.exists()) {
                    boolean created = dir.mkdirs();
                    log.info("Created directory: {} - success: {}", dir.getAbsolutePath(), created);
                    if (!created) continue; // Skip to next directory if creation failed
                }

                // Test if we can write to this directory
                if (isDirectoryWritable(dir)) {
                    emailDir = dir;
                    foundWritableDir = true;
                    log.info("Using directory for emails: {}", emailDir.getAbsolutePath());
                    break;
                } else {
                    log.warn("Directory is not writable: {}", dir.getAbsolutePath());
                }
            } catch (Exception e) {
                log.warn("Could not use directory {}: {}", dir.getAbsolutePath(), e.getMessage());
            }
        }

        // If we couldn't find a writable directory, create one in the temp directory with a timestamp
        if (!foundWritableDir) {
            try {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                emailDir = new File(System.getProperty("java.io.tmpdir"), "emails_" + timestamp);
                boolean created = emailDir.mkdirs();
                log.info("Created fallback directory with timestamp: {} - success: {}", emailDir.getAbsolutePath(), created);

                if (!created || !isDirectoryWritable(emailDir)) {
                    log.error("Could not create a writable directory for emails!");
                }
            } catch (Exception e) {
                log.error("Failed to create fallback directory: {}", e.getMessage(), e);
            }
        }

        // Final check and logging
        if (emailDir != null && emailDir.exists() && isDirectoryWritable(emailDir)) {
            log.info("LocalEmailSender initialized successfully. Emails will be saved to: {}", emailDir.getAbsolutePath());
        } else {
            log.error("LocalEmailSender initialization FAILED! No writable directory found.");
            if (emailDir != null) {
                log.error("Last attempted directory: {}, exists: {}, writable: {}",
                        emailDir.getAbsolutePath(),
                        emailDir.exists(),
                        emailDir.exists() ? isDirectoryWritable(emailDir) : "N/A");
            }
        }
    }

    /**
     * Check if a directory is writable by creating a temporary file
     */
    private boolean isDirectoryWritable(File directory) {
        try {
            File tempFile = File.createTempFile("test", ".tmp", directory);
            boolean canWrite = tempFile.exists();
            tempFile.delete();
            return canWrite;
        } catch (IOException e) {
            log.warn("Directory is not writable: {}", directory.getAbsolutePath());
            return false;
        }
    }

    /**
     * Save an email to a local file
     */
    public boolean sendEmail(String to, String subject, String body) {
        // Always create a desktop directory as a fallback
        File desktopDir = new File(System.getProperty("user.home"), "Desktop");
        File emailsOnDesktop = new File(desktopDir, "StudentPortalEmails");

        if (!emailsOnDesktop.exists()) {
            emailsOnDesktop.mkdirs();
            log.info("Created emails directory on desktop: {}", emailsOnDesktop.getAbsolutePath());
        }

        // If the main email directory is not available, use the desktop directory
        if (emailDir == null || !emailDir.exists() || !isDirectoryWritable(emailDir)) {
            log.warn("Main email directory not available, using desktop directory: {}", emailsOnDesktop.getAbsolutePath());
            emailDir = emailsOnDesktop;
        }

        File emailFile = null;
        try {
            log.info("Saving email to local file for: {} with subject: {}", to, subject);

            // Create a timestamp for the filename
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
            String timestamp = sdf.format(new Date());

            // Sanitize the email address for use in a filename
            String safeEmail = to.replace("@", "_at_")
                               .replace(".", "_")
                               .replace(" ", "_")
                               .replace("/", "_")
                               .replace("\\", "_");

            // Create a filename based on the recipient and timestamp
            emailFile = new File(emailDir, "email_" + safeEmail + "_" + timestamp + ".txt");

            // Also create a copy on the desktop for easy access
            File desktopCopy = new File(emailsOnDesktop, "email_" + safeEmail + "_" + timestamp + ".txt");

            // Create the email content with more details
            StringBuilder emailContent = new StringBuilder();
            emailContent.append("From: ").append(FROM_EMAIL).append("\n");
            emailContent.append("To: ").append(to).append("\n");
            emailContent.append("Subject: ").append(subject).append("\n");
            emailContent.append("Date: ").append(new Date()).append("\n");
            emailContent.append("Saved: ").append(timestamp).append("\n");
            emailContent.append("File: ").append(emailFile.getAbsolutePath()).append("\n");
            emailContent.append("\n");
            emailContent.append(body);
            emailContent.append("\n\n");
            emailContent.append("--- End of Email ---");

            // Write the email to the main file
            try (FileWriter writer = new FileWriter(emailFile)) {
                writer.write(emailContent.toString());
                writer.flush();
                log.info("Email saved successfully to file: {}", emailFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to save email to main file: {}", e.getMessage());
            }

            // Also write to the desktop copy
            try (FileWriter desktopWriter = new FileWriter(desktopCopy)) {
                desktopWriter.write(emailContent.toString());
                desktopWriter.flush();
                log.info("Email saved successfully to desktop: {}", desktopCopy.getAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to save email to desktop: {}", e.getMessage());
            }

            // Print the reset link to the console for easy access
            if (subject.contains("Reset Password") && body.contains("http")) {
                String resetLink = body.substring(body.indexOf("http"), body.indexOf("\n", body.indexOf("http")));
                log.info("\n\n==================================================\n" +
                         "PASSWORD RESET LINK: {}\n" +
                         "==================================================\n", resetLink);

                // Create a special file with just the reset link for easy access
                File resetLinkFile = new File(emailsOnDesktop, "RESET_LINK_" + safeEmail + ".txt");
                try (FileWriter linkWriter = new FileWriter(resetLinkFile)) {
                    linkWriter.write("Reset link for " + to + ":\n\n" + resetLink);
                    linkWriter.flush();
                    log.info("Reset link saved to: {}", resetLinkFile.getAbsolutePath());
                } catch (IOException e) {
                    log.error("Failed to save reset link to file: {}", e.getMessage());
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Unexpected error saving email: {}", e.getMessage(), e);
            if (emailFile != null) {
                log.error("Attempted to write to: {}", emailFile.getAbsolutePath());
            }
            return false;
        }
    }

    /**
     * Try to send an email using a local SMTP server if available
     * Falls back to saving to a file if no server is available
     */
    public boolean trySendViaLocalSmtp(String to, String subject, String body) {
        try {
            log.info("Attempting to send email via local SMTP to {} with subject: {}", to, subject);

            // Set up mail server properties for local SMTP
            Properties props = new Properties();
            props.put("mail.smtp.host", "localhost");
            props.put("mail.smtp.port", "25");
            props.put("mail.smtp.auth", "false");

            // Create a session without authentication
            Session session = Session.getInstance(props);

            // Enable debugging
            session.setDebug(true);

            // Create a message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            try {
                // Try to send the message
                Transport.send(message);
                log.info("Email sent successfully via local SMTP to: {}", to);
                return true;
            } catch (MessagingException e) {
                log.warn("Could not send via local SMTP, falling back to file: {}", e.getMessage());
                // Fall back to saving to a file
                return sendEmail(to, subject, body);
            }
        } catch (MessagingException e) {
            log.error("Failed to create email message: {}", e.getMessage(), e);
            // Fall back to saving to a file
            return sendEmail(to, subject, body);
        }
    }

    /**
     * Send a password reset email
     */
    public boolean sendPasswordResetEmail(String email, String token) {
        if (email == null || email.trim().isEmpty()) {
            log.error("Cannot send password reset email: Email address is null or empty");
            return false;
        }

        if (token == null || token.trim().isEmpty()) {
            log.error("Cannot send password reset email: Token is null or empty");
            return false;
        }

        log.info("Sending password reset email to: {} with token: {}", email, token);

        // Create the reset link with the token
        String resetLink = "http://localhost:8081/reset-password?token=" + token;

        // Create a more detailed and formatted email
        String subject = "🔒 Reset Password - Student Portal";
        String body = "Dear Student,\n\n" +
                "You have requested to reset your password. Please click the link below to set a new password:\n\n" +
                resetLink + "\n\n" +
                "If you did not request this password reset, please ignore this email.\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "Regards,\nStudent Portal Team";

        // Always save to file first to ensure we have a record
        boolean savedToFile = sendEmail(email, subject, body);

        if (savedToFile) {
            // Print the reset link prominently in the logs for easy access
            log.info("\n\n==================================================\n" +
                     "PASSWORD RESET LINK GENERATED\n" +
                     "--------------------------------------------------\n" +
                     "Email: {}\n" +
                     "Token: {}\n" +
                     "Link:  {}\n" +
                     "==================================================\n",
                     email, token, resetLink);
        } else {
            log.error("Failed to save password reset email to file!");
        }

        // Then try SMTP as a bonus if available, but don't let it affect the result
        // if we already saved to a file successfully
        try {
            boolean sentViaSmtp = trySendViaLocalSmtp(email, subject, body);
            log.info("Password reset email sent via SMTP: {}", sentViaSmtp);

            // If file save failed but SMTP worked, that's still a success
            if (!savedToFile && sentViaSmtp) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Failed to send via SMTP: {}", e.getMessage());
            // If SMTP failed but we saved to file, that's still a success
            if (savedToFile) {
                log.info("Email saved to file as fallback");
            }
        }

        return savedToFile; // Return true if we at least saved to a file
    }

    /**
     * Send a teacher password reset email
     */
    public boolean sendTeacherPasswordResetEmail(String email, String token) {
        String resetLink = "http://localhost:8081/teacher/reset-password?token=" + token;
        String subject = "🔒 Reset Password - Teacher Portal";
        String body = "Dear Teacher,\n\n" +
                "You have requested to reset your password. Please click the link below to set a new password:\n\n" +
                resetLink + "\n\n" +
                "If you did not request this password reset, please ignore this email.\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "Regards,\nTeacher Portal Team";

        return trySendViaLocalSmtp(email, subject, body);
    }

    /**
     * Send an OTP email
     */
    public boolean sendOtpToEmail(String email, String otp) {
        String subject = "🔐 OTP Verification - Faculty Signup";
        String body = "Dear HOD,\n\n" +
                "Your OTP for faculty registration verification is: " + otp + "\n\n" +
                "This OTP will be used to verify a new faculty signup request.\n" +
                "It will expire in 10 minutes.\n\n" +
                "If you did not request this OTP, please ignore this email.\n\n" +
                "Regards,\nTeacher Portal Team";

        return trySendViaLocalSmtp(email, subject, body);
    }
}
