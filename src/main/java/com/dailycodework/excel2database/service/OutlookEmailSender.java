package com.dailycodework.excel2database.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Email sender that uses Outlook/Hotmail SMTP server
 */
@Service
@Slf4j
public class OutlookEmailSender {

    // Default credentials - replace these with your actual Outlook/Hotmail credentials
    private final String username = "your-outlook-email@outlook.com";
    private final String password = "your-outlook-password";

    /**
     * Send an email using Outlook/Hotmail SMTP server
     */
    public boolean sendEmail(String to, String subject, String body) {
        try {
            log.info("Attempting to send email via Outlook to {} with subject: {}", to, subject);
            
            // Set up mail server properties for Outlook
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.office365.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", "smtp.office365.com");
            
            // Create a session with an authenticator
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            
            // Enable debugging
            session.setDebug(true);
            
            // Create a message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            
            // Send the message
            Transport.send(message);
            
            log.info("Email sent successfully via Outlook to: {}", to);
            return true;
        } catch (MessagingException e) {
            log.error("Failed to send email via Outlook: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send a password reset email
     */
    public boolean sendPasswordResetEmail(String email, String token) {
        String resetLink = "http://localhost:8081/reset-password?token=" + token;
        String subject = "🔒 Reset Password - Student Portal";
        String body = "Dear Student,\n\n" +
                "You have requested to reset your password. Please click the link below to set a new password:\n\n" +
                resetLink + "\n\n" +
                "If you did not request this password reset, please ignore this email.\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "Regards,\nStudent Portal Team";
        
        return sendEmail(email, subject, body);
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
        
        return sendEmail(email, subject, body);
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
        
        return sendEmail(email, subject, body);
    }
}
