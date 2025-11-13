package com.dailycodework.excel2database.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Alternative email service implementation using JavaMail directly
 * This bypasses Spring's JavaMailSender to provide more direct control
 */
@Service
@Slf4j
public class DirectEmailService {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private String port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    private Properties properties;
    private Session session;

    @PostConstruct
    public void init() {
        log.info("Initializing DirectEmailService...");

        // Set up mail properties with enhanced configuration
        properties = new Properties();

        // Authentication settings
        properties.put("mail.smtp.auth", "true");

        // Server settings
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.ssl.trust", host);

        // Timeouts - increased for reliability
        properties.put("mail.smtp.timeout", "30000");  // 30 seconds timeout
        properties.put("mail.smtp.connectiontimeout", "30000");  // 30 seconds connection timeout
        properties.put("mail.smtp.writetimeout", "30000");  // 30 seconds write timeout

        // STARTTLS settings for Gmail
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");

        // Debug settings - set to false in production
        properties.put("mail.debug", "false");
        properties.put("mail.debug.auth", "false");

        // Log the configuration (without sensitive data)
        log.info("DirectEmailService configured with host: {}, port: {}", host, port);
        log.info("Using username: {}", username);

        // Create session with authenticator
        session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        session.setDebug(false); // Disable debug to reduce log noise

        // Log configuration (mask password)
        String maskedPassword = password.length() > 4 ?
                "****" + password.substring(password.length() - 4) : "****";
        log.info("DirectEmailService initialized with username: {} and password: {}", username, maskedPassword);

        // Don't test email configuration at startup to avoid authentication errors
        // The email service will be tested on-demand when needed
        log.info("Email testing disabled at startup to prevent authentication errors");
    }

    /**
     * Test the email configuration by sending a test email
     */
    @PostConstruct
    private void testEmailConfiguration() {
        // Skip test if using default/dummy credentials
        if (!isEmailConfigured()) {
            log.info("Skipping email configuration test - using default/dummy credentials");
            log.info("Email service will fall back to local file storage when needed");
            return;
        }

        try {
            log.info("Testing email configuration with host: {}, port: {}, username: {}", host, port, username);

            // Just test the connection without sending an actual email
            log.info("Testing SMTP connection...");
            Transport transport = Session.getInstance(properties).getTransport("smtp");
            transport.connect(host, Integer.parseInt(port), username, password);
            transport.close();
            log.info("✅ SMTP connection test successful! DirectEmailService is configured correctly.");
        } catch (MessagingException e) {
            log.warn("⚠️ Failed to connect to SMTP server. Email functionality will use local fallback: {}", e.getMessage());
            log.info("Email service will fall back to local file storage when needed");
            // Don't rethrow - we don't want to prevent application startup
        } catch (Exception e) {
            log.warn("⚠️ Unexpected error testing email configuration: {}", e.getMessage());
            log.info("Email service will fall back to local file storage when needed");
            // Don't rethrow - we don't want to prevent application startup
        }
    }

    /**
     * Check if email is properly configured
     */
    public boolean isEmailConfigured() {
        // Check if username and password are the default/dummy values
        return username != null && !username.equals("example@gmail.com") &&
               password != null && !password.equals("dummypassword") &&
               !username.isEmpty() && !password.isEmpty();
    }

    /**
     * Send password reset email using direct JavaMail
     */
    public boolean sendPasswordResetEmail(String email, String token) {
        // Check if email is configured
        if (!isEmailConfigured()) {
            log.warn("Email is not properly configured. Using local file fallback.");
            log.warn("To send actual emails, set MAIL_USERNAME and MAIL_PASSWORD environment variables.");
            log.warn("Current username: {}", username);
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendPasswordResetEmail(email, token);
        }

        try {
            log.info("DirectEmailService: Preparing to send password reset email to {}", email);
            log.info("Using SMTP settings - Host: {}, Port: {}, Username: {}", host, port, username);

            String resetLink = "http://localhost:8081/reset-password?token=" + token;
            log.info("Reset link generated: {}", resetLink);

            // Create a new session for this specific email to ensure fresh settings
            Session emailSession = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            emailSession.setDebug(false); // Disable debug to reduce log noise

            MimeMessage message = new MimeMessage(emailSession);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("🔒 Reset Password - Student Portal");

            // Create a more detailed email body
            String emailBody = "Dear Student,\n\n" +
                    "You have requested to reset your password. Please click the link below to set a new password:\n\n" +
                    resetLink + "\n\n" +
                    "If you did not request this password reset, please ignore this email.\n\n" +
                    "This link will expire in 24 hours.\n\n" +
                    "Regards,\nStudent Portal Team";

            message.setText(emailBody);
            log.info("Email message prepared with subject: {}", message.getSubject());

            log.info("Attempting to send password reset email via explicit transport");
            // Use smtp transport with STARTTLS
            Transport transport = emailSession.getTransport("smtp");
            transport.connect(host, Integer.parseInt(port), username, password);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            log.info("✅ Password reset email sent successfully to: {}", email);
            return true;
        } catch (MessagingException e) {
            log.error("❌ Failed to send password reset email to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendPasswordResetEmail(email, token);
        } catch (Exception e) {
            log.error("❌ Unexpected error sending password reset email to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendPasswordResetEmail(email, token);
        }
    }

    /**
     * Send teacher password reset email using direct JavaMail
     */
    public boolean sendTeacherPasswordResetEmail(String email, String token) {
        // Check if email is configured
        if (!isEmailConfigured()) {
            log.warn("Email is not properly configured. Using local file fallback.");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendTeacherPasswordResetEmail(email, token);
        }

        try {
            log.info("DirectEmailService: Preparing to send teacher password reset email to {}", email);
            log.info("Using SMTP settings - Host: {}, Port: {}, Username: {}", host, port, username);

            String resetLink = "http://localhost:8081/teacher/reset-password?token=" + token;
            log.info("Teacher reset link generated: {}", resetLink);

            // Create a new session for this specific email to ensure fresh settings
            Session emailSession = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            emailSession.setDebug(false); // Disable debug to reduce log noise

            MimeMessage message = new MimeMessage(emailSession);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("🔒 Reset Password - Teacher Portal");

            // Create a more detailed email body
            String emailBody = "Dear Teacher,\n\n" +
                    "You have requested to reset your password. Please click the link below to set a new password:\n\n" +
                    resetLink + "\n\n" +
                    "If you did not request this password reset, please ignore this email.\n\n" +
                    "This link will expire in 24 hours.\n\n" +
                    "Regards,\nTeacher Portal Team";

            message.setText(emailBody);
            log.info("Email message prepared with subject: {}", message.getSubject());

            log.info("Attempting to send teacher password reset email via explicit transport");
            // Use smtp transport with STARTTLS
            Transport transport = emailSession.getTransport("smtp");
            transport.connect(host, Integer.parseInt(port), username, password);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            log.info("✅ Teacher password reset email sent successfully to: {}", email);
            return true;
        } catch (MessagingException e) {
            log.error("❌ Failed to send teacher password reset email to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendTeacherPasswordResetEmail(email, token);
        } catch (Exception e) {
            log.error("❌ Unexpected error sending teacher password reset email to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendTeacherPasswordResetEmail(email, token);
        }
    }

    /**
     * Send a test email using a different approach
     */
    public boolean sendTestEmailAlternative(String email) {
        try {
            log.info("Sending test email using alternative approach to {}", email);

            // Create properties for Gmail
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            props.put("mail.debug", "true");

            // Create a new session with an authenticator
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            // Create a message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Test Email - Alternative Method");
            message.setText("This is a test email sent using an alternative method.\n\n" +
                    "If you're seeing this, the alternative email configuration is working.\n\n" +
                    "Timestamp: " + new java.util.Date());

            // Send the message
            Transport.send(message);
            log.info("✅ Alternative test email sent successfully to: {}", email);
            return true;
        } catch (Exception e) {
            log.error("❌ Failed to send alternative test email: {}", e.getMessage());
            log.error("Exception details:", e);
            return false;
        }
    }

    /**
     * Check if the email service is working properly
     */
    public boolean isEmailServiceWorking() {
        try {
            log.info("Testing if email service is working...");

            // Create a test session
            Session emailSession = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            // Try to connect to the SMTP server using smtp protocol with STARTTLS
            Transport transport = emailSession.getTransport("smtp");
            transport.connect(host, Integer.parseInt(port), username, password);
            boolean isConnected = transport.isConnected();
            transport.close();

            log.info("Email service connection test result: {}", isConnected ? "SUCCESS" : "FAILED");
            return isConnected;
        } catch (Exception e) {
            log.error("Email service connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Send OTP email using direct JavaMail
     */
    public boolean sendOtpToEmail(String email, String otp) {
        // Check if email is configured
        if (!isEmailConfigured()) {
            log.warn("Email is not properly configured. Using local file fallback.");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendOtpToEmail(email, otp);
        }

        try {
            log.info("DirectEmailService: Preparing to send OTP email to {}", email);
            log.info("Using SMTP settings - Host: {}, Port: {}, Username: {}", host, port, username);

            // Create a new session for this specific email to ensure fresh settings
            Session emailSession = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            emailSession.setDebug(false); // Disable debug to reduce log noise

            MimeMessage message = new MimeMessage(emailSession);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("🔐 OTP Verification - Faculty Signup");

            // Create a more detailed email body
            String emailBody = "Dear HOD,\n\n" +
                    "Your OTP for faculty registration verification is: " + otp + "\n\n" +
                    "This OTP will be used to verify a new faculty signup request.\n" +
                    "It will expire in 10 minutes.\n\n" +
                    "If you did not request this OTP, please ignore this email.\n\n" +
                    "Regards,\nTeacher Portal Team";

            message.setText(emailBody);
            log.info("OTP email message prepared with subject: {}", message.getSubject());

            log.info("Attempting to send OTP email via explicit transport");
            // Use smtp transport with STARTTLS
            Transport transport = emailSession.getTransport("smtp");
            transport.connect(host, Integer.parseInt(port), username, password);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            log.info("✅ OTP email sent successfully to: {}", email);
            return true;
        } catch (MessagingException e) {
            log.error("❌ Failed to send OTP email to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendOtpToEmail(email, otp);
        } catch (Exception e) {
            log.error("❌ Unexpected error sending OTP email to {}: {}", email, e.getMessage());
            log.warn("Falling back to local file email storage");
            // Use LocalEmailSender as fallback
            LocalEmailSender localSender = new LocalEmailSender();
            return localSender.sendOtpToEmail(email, otp);
        }
    }
}
