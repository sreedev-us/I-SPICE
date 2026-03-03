package com.company.I_SPICE.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String token, String name) {
        String url = baseUrl + "/verify-email?token=" + token;

        String subject = "Verify Your I-SPICE Account";
        String content = "<html><body style='font-family: Arial, sans-serif;'>"
                + "<h2>Welcome to I-SPICE, " + name + "!</h2>"
                + "<p>Thank you for registering. Please click the button below to verify your email address:</p>"
                + "<a href='" + url
                + "' style='display:inline-block; padding:10px 20px; color:white; background-color:#2e7d32; text-decoration:none; border-radius:5px;'>Verify Email</a>"
                + "<p>If the button doesn't work, copy and paste this link into your browser:</p>"
                + "<p>" + url + "</p>"
                + "</body></html>";

        sendHtmlEmail(to, subject, content);
    }

    private void sendHtmlEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            // Allow bypassing email send in dev if credentials aren't set
            try {
                mailSender.send(message);
                logger.info("Email sent successfully to {}", to);
            } catch (Exception e) {
                logger.warn("Failed to send email to {}. Missing SMTP credentials? Link generated: \n{}", to, content);
            }

        } catch (MessagingException e) {
            logger.error("Failed to construct email", e);
        }
    }
}
