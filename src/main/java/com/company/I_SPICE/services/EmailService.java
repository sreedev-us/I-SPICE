package com.company.I_SPICE.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationEmail(String to, String token, String name) {
        String url = baseUrl + "/verify-email?token=" + token;

        String subject = "Verify Your I-SPICE Account";
        String content = "<html><body style='margin:0;padding:0;background:#f9f7f0;font-family:Arial,sans-serif;'>"
                + "<div style='max-width:560px;margin:40px auto;background:#fff;border-radius:16px;overflow:hidden;border:1px solid #f0e6d3;box-shadow:0 8px 24px rgba(212,175,55,0.1);'>"
                + "<div style='background:linear-gradient(135deg,#2e7d32,#D4AF37);padding:32px 36px;text-align:center;'>"
                + "<h1 style='margin:0;color:white;font-size:26px;letter-spacing:1px;'>I-SPICE</h1>"
                + "<p style='color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:13px;'>Premium Plant-Based Products</p>"
                + "</div>"
                + "<div style='padding:36px;'>"
                + "<h2 style='color:#333;margin-top:0;'>Welcome to I-SPICE!</h2>"
                + "<p style='color:#666;line-height:1.6;'>Hi <strong>" + name + "</strong>,</p>"
                + "<p style='color:#666;line-height:1.6;'>Thank you for creating an account. Before you can log in and start shopping, please verify your email address by clicking the button below:</p>"
                + "<div style='text-align:center;margin:32px 0;'>"
                + "<a href='" + url + "' style='display:inline-block;padding:14px 32px;color:white;"
                + "background:linear-gradient(to right,#2e7d32,#D4AF37);text-decoration:none;"
                + "border-radius:8px;font-size:16px;font-weight:600;letter-spacing:0.5px;'>"
                + "Verify Email Address</a>"
                + "</div>"
                + "<p style='color:#999;font-size:13px;line-height:1.6;'>If the button doesn't work, copy and paste this link into your browser:</p>"
                + "<p style='color:#2e7d32;font-size:12px;word-break:break-all;'>" + url + "</p>"
                + "<hr style='border:none;border-top:1px solid #f0e6d3;margin:28px 0;'/>"
                + "<p style='color:#bbb;font-size:12px;'>If you didn't create an account, you can safely ignore this email.</p>"
                + "</div>"
                + "</div></body></html>";

        sendHtmlEmail(to, subject, content);
    }

    @Async
    public void sendPasswordResetEmail(String to, String token, String name) {
        String url = baseUrl + "/reset-password?token=" + token;

        String subject = "Reset Your I-SPICE Password";
        String content = "<html><body style='margin:0;padding:0;background:#f9f7f0;font-family:Arial,sans-serif;'>"
                + "<div style='max-width:560px;margin:40px auto;background:#fff;border-radius:16px;overflow:hidden;border:1px solid #f0e6d3;box-shadow:0 8px 24px rgba(212,175,55,0.1);'>"
                + "<div style='background:linear-gradient(135deg,#2e7d32,#D4AF37);padding:32px 36px;text-align:center;'>"
                + "<h1 style='margin:0;color:white;font-size:26px;letter-spacing:1px;'>I-SPICE</h1>"
                + "<p style='color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:13px;'>Premium Plant-Based Products</p>"
                + "</div>"
                + "<div style='padding:36px;'>"
                + "<h2 style='color:#333;margin-top:0;'>Reset Your Password</h2>"
                + "<p style='color:#666;line-height:1.6;'>Hi <strong>" + name + "</strong>,</p>"
                + "<p style='color:#666;line-height:1.6;'>We received a request to reset your I-SPICE account password. "
                + "Click the button below to set a new password. This link is valid for <strong>1 hour</strong>.</p>"
                + "<div style='text-align:center;margin:32px 0;'>"
                + "<a href='" + url + "' style='display:inline-block;padding:14px 32px;color:white;"
                + "background:linear-gradient(to right,#2e7d32,#D4AF37);text-decoration:none;"
                + "border-radius:8px;font-size:16px;font-weight:600;letter-spacing:0.5px;'>"
                + "Reset Password</a>"
                + "</div>"
                + "<p style='color:#999;font-size:13px;line-height:1.6;'>If the button doesn't work, copy and paste this link:</p>"
                + "<p style='color:#2e7d32;font-size:12px;word-break:break-all;'>" + url + "</p>"
                + "<hr style='border:none;border-top:1px solid #f0e6d3;margin:28px 0;'/>"
                + "<p style='color:#bbb;font-size:12px;'>If you didn't request a password reset, you can safely ignore this email. "
                + "Your password will not change.</p>"
                + "</div>"
                + "</div></body></html>";

        sendHtmlEmail(to, subject, content);
    }

    private void sendHtmlEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            // Allow bypassing email send in dev if credentials aren't set
            try {
                mailSender.send(message);
                logger.info("Email sent successfully to {}", to);
            } catch (Exception e) {
                logger.error("Failed to send email to {}. Exception: {}", to, e.getMessage(), e);
                logger.warn("Fallback link generated: \n{}", content);
            }

        } catch (MessagingException e) {
            logger.error("Failed to construct email", e);
        }
    }
}
