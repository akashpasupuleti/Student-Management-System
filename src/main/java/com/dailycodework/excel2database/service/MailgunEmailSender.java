package com.dailycodework.excel2database.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Email sender that uses Mailgun API
 * This is a simple implementation that uses the Mailgun API directly via HTTP
 */
@Service
@Slf4j
public class MailgunEmailSender {

    // Default credentials - replace these with your actual Mailgun credentials
    private final String API_KEY = "your-mailgun-api-key";
    private final String DOMAIN = "your-mailgun-domain";
    private final String FROM_EMAIL = "noreply@your-mailgun-domain";
    private final String FROM_NAME = "Student Portal";

    /**
     * Send an email using Mailgun API
     */
    public boolean sendEmail(String to, String subject, String body) {
        try {
            log.info("Attempting to send email via Mailgun to {} with subject: {}", to, subject);

            // Create the URL for the Mailgun API
            URL url = URI.create("https://api.mailgun.net/v3/" + DOMAIN + "/messages").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            // Set basic authentication header
            String auth = "api:" + API_KEY;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

            // Set content type
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Enable output and disable caching
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // Create the form data
            String formData = "from=" + FROM_NAME + " <" + FROM_EMAIL + ">" +
                    "&to=" + to +
                    "&subject=" + subject +
                    "&text=" + body;

            // Send the request
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(formData);
                wr.flush();
            }

            // Get the response
            int responseCode = connection.getResponseCode();

            // Read the response
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

            // Check if the request was successful
            boolean success = responseCode >= 200 && responseCode < 300;
            if (success) {
                log.info("Email sent successfully via Mailgun to: {}", to);
            } else {
                log.error("Failed to send email via Mailgun. Response code: {}, Response: {}",
                        responseCode, response.toString());
            }

            return success;
        } catch (Exception e) {
            log.error("Failed to send email via Mailgun: {}", e.getMessage(), e);
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
