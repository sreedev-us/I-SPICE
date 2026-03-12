package com.company.I_SPICE.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestTemplate;

import com.company.I_SPICE.model.Order;
import com.company.I_SPICE.model.OrderItem;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.password}")
    private String brevoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public EmailService() {
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

    @Async
    public void sendOrderConfirmationEmail(Order order) {
        String to = order.getUser().getEmail();
        String name = order.getUser().getFirstName() != null ? order.getUser().getFirstName() : order.getUser().getUsername();
        String subject = "Order Confirmed - #" + order.getOrderNumber();

        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem item : order.getOrderItems()) {
            itemsHtml.append("<tr style='border-bottom:1px solid #f0e6d3;'>")
                    .append("<td style='padding:12px 0;color:#333;'>")
                    .append("<div style='font-weight:600;'>").append(item.getProductName()).append("</div>")
                    .append("<div style='font-size:12px;color:#999;'>Category: ").append(item.getProductCategory()).append("</div>")
                    .append("</td>")
                    .append("<td style='padding:12px 0;text-align:center;color:#666;'>").append(item.getQuantity()).append("</td>")
                    .append("<td style='padding:12px 0;text-align:right;color:#333;font-weight:600;'>₹").append(item.getTotalPrice()).append("</td>")
                    .append("</tr>");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        String orderDate = order.getOrderDate().format(formatter);

        String content = "<html><body style='margin:0;padding:0;background:#f9f7f0;font-family:Arial,sans-serif;'>"
                + "<div style='max-width:600px;margin:40px auto;background:#fff;border-radius:16px;overflow:hidden;border:1px solid #f0e6d3;box-shadow:0 8px 24px rgba(212,175,55,0.1);'>"
                + "<div style='background:linear-gradient(135deg,#2e7d32,#D4AF37);padding:32px 36px;text-align:center;'>"
                + "<h1 style='margin:0;color:white;font-size:26px;letter-spacing:1px;'>I-SPICE</h1>"
                + "<p style='color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:13px;'>Thank you for your purchase!</p>"
                + "</div>"
                + "<div style='padding:36px;'>"
                + "<h2 style='color:#333;margin-top:0;'>Order Confirmation</h2>"
                + "<p style='color:#666;line-height:1.6;'>Hi <strong>" + name + "</strong>,</p>"
                + "<p style='color:#666;line-height:1.6;'>Great news! Your order <strong>#" + order.getOrderNumber() + "</strong> has been confirmed and is being prepared for shipment.</p>"
                
                + "<div style='margin:32px 0;padding:24px;background:#fdfbf7;border-radius:12px;border:1px solid #f0e6d3;'>"
                + "<h3 style='margin:0 0 16px;color:#2e7d32;font-size:16px;text-transform:uppercase;letter-spacing:1px;'>Order Summary</h3>"
                + "<table style='width:100%;border-collapse:collapse;'>"
                + "<thead><tr style='border-bottom:2px solid #f0e6d3;'>"
                + "<th style='text-align:left;padding-bottom:12px;color:#999;font-size:12px;'>PRODUCT</th>"
                + "<th style='text-align:center;padding-bottom:12px;color:#999;font-size:12px;'>QTY</th>"
                + "<th style='text-align:right;padding-bottom:12px;color:#999;font-size:12px;'>PRICE</th>"
                + "</tr></thead>"
                + "<tbody>" + itemsHtml.toString() + "</tbody>"
                + "<tfoot>"
                + "<tr><td colspan='2' style='padding-top:20px;color:#666;'>Subtotal</td><td style='padding-top:20px;text-align:right;color:#333;'>₹" + order.getSubtotal() + "</td></tr>"
                + "<tr><td colspan='2' style='padding:8px 0;color:#666;'>Shipping</td><td style='padding:8px 0;text-align:right;color:#333;'>₹" + order.getShipping() + "</td></tr>"
                + "<tr><td colspan='2' style='padding:8px 0;color:#666;'>GST (18%)</td><td style='padding:8px 0;text-align:right;color:#333;'>₹" + order.getTax() + "</td></tr>"
                + "<tr style='font-size:18px;font-weight:bold;'>"
                + "<td colspan='2' style='padding-top:16px;color:#2e7d32;'>Total Amount</td>"
                + "<td style='padding-top:16px;text-align:right;color:#2e7d32;'>₹" + order.getTotal() + "</td>"
                + "</tr>"
                + "</tfoot></table>"
                + "</div>"

                + "<div style='display:grid;grid-template-columns:1fr 1fr;gap:24px;margin-bottom:32px;'>"
                + "<div>"
                + "<h4 style='margin:0 0 8px;color:#333;font-size:14px;'>Shipping Address</h4>"
                + "<p style='margin:0;color:#666;font-size:13px;line-height:1.5;'>" + order.getShippingAddress().replace("\n", "<br>") + "</p>"
                + "</div>"
                + "<div>"
                + "<h4 style='margin:0 0 8px;color:#333;font-size:14px;'>Order Details</h4>"
                + "<p style='margin:0;color:#666;font-size:13px;line-height:1.5;'>"
                + "Order Date: " + orderDate + "<br>"
                + "Payment: " + order.getPaymentMethod() + "<br>"
                + "Status: " + order.getPaymentStatus()
                + "</p>"
                + "</div>"
                + "</div>"

                + "<div style='text-align:center;margin:32px 0;'>"
                + "<a href='" + baseUrl + "/orders' style='display:inline-block;padding:14px 32px;color:white;"
                + "background:linear-gradient(to right,#2e7d32,#D4AF37);text-decoration:none;"
                + "border-radius:8px;font-size:16px;font-weight:600;letter-spacing:0.5px;'>"
                + "Track Your Order</a>"
                + "</div>"

                + "<hr style='border:none;border-top:1px solid #f0e6d3;margin:28px 0;'/>"
                + "<p style='color:#999;font-size:12px;text-align:center;'>If you have any questions, please contact our support team at support@i-spice.com</p>"
                + "</div>"
                + "</div></body></html>";

        sendHtmlEmail(to, subject, content);
    }

    @Async
    public void sendOrderCancellationEmail(Order order) {
        String to = order.getUser().getEmail();
        String name = order.getUser().getFirstName() != null ? order.getUser().getFirstName() : order.getUser().getUsername();
        String subject = "Order Cancelled - #" + order.getOrderNumber();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        String orderDate = order.getOrderDate().format(formatter);

        String content = "<html><body style='margin:0;padding:0;background:#f9f7f0;font-family:Arial,sans-serif;'>"
                + "<div style='max-width:600px;margin:40px auto;background:#fff;border-radius:16px;overflow:hidden;border:1px solid #f0e6d3;box-shadow:0 8px 24px rgba(212,175,55,0.1);'>"
                + "<div style='background:linear-gradient(135deg,#666,#999);padding:32px 36px;text-align:center;'>"
                + "<h1 style='margin:0;color:white;font-size:26px;letter-spacing:1px;'>I-SPICE</h1>"
                + "<p style='color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:13px;'>Order Cancellation Notice</p>"
                + "</div>"
                + "<div style='padding:36px;'>"
                + "<h2 style='color:#333;margin-top:0;'>Order Cancelled</h2>"
                + "<p style='color:#666;line-height:1.6;'>Hi <strong>" + name + "</strong>,</p>"
                + "<p style='color:#666;line-height:1.6;'>This email is to confirm that your order <strong>#" + order.getOrderNumber() + "</strong> (placed on " + orderDate + ") has been cancelled.</p>"

                + "<div style='margin:32px 0;padding:24px;background:#fff8f8;border-radius:12px;border:1px solid #f8d7da;'>"
                + "<h3 style='margin:0 0 12px;color:#721c24;font-size:16px;text-transform:uppercase;letter-spacing:1px;'>We're Sorry to See You Go</h3>"
                + "<p style='color:#666;font-size:14px;line-height:1.6;margin:0;'>"
                + "We would love to know if there was anything specific that led to this cancellation. "
                + "Your feedback helps us grow and serve you better. Was it the price, delivery time, or something else?"
                + "</p>"
                + "<div style='margin-top:20px;text-align:center;'>"
                + "<a href='mailto:support@i-spice.com?subject=Feedback for Order " + order.getOrderNumber() + "' style='display:inline-block;padding:10px 24px;color:#721c24;border:1px solid #721c24;text-decoration:none;border-radius:6px;font-size:14px;font-weight:600;'>"
                + "Share Your Feedback</a>"
                + "</div>"
                + "</div>"

                + "<p style='color:#666;line-height:1.6;'>At <strong>I-SPICE</strong>, we are constantly striving to improve our products and services. "
                + "We hope to have the opportunity to bring our premium plant-based products to your doorstep again in the future.</p>"

                + "<div style='margin:32px 0;padding:24px;background:#fdfbf7;border-radius:12px;border:1px solid #f0e6d3;'>"
                + "<h4 style='margin:0 0 12px;color:#333;font-size:14px;'>Refund Information</h4>"
                + "<p style='margin:0;color:#666;font-size:13px;line-height:1.5;'>"
                + "If you have already paid for this order, the refund process has been initiated. "
                + "Depending on your bank, it may take 5-7 business days to reflect in your account."
                + "</p>"
                + "</div>"

                + "<div style='text-align:center;margin:32px 0;'>"
                + "<a href='" + baseUrl + "/shop' style='display:inline-block;padding:14px 32px;color:white;"
                + "background:linear-gradient(to right,#2e7d32,#D4AF37);text-decoration:none;"
                + "border-radius:8px;font-size:16px;font-weight:600;letter-spacing:0.5px;'>"
                + "Continue Shopping</a>"
                + "</div>"

                + "<hr style='border:none;border-top:1px solid #f0e6d3;margin:28px 0;'/>"
                + "<p style='color:#999;font-size:12px;text-align:center;'>If this cancellation was a mistake, please contact us immediately.</p>"
                + "</div>"
                + "</div></body></html>";

        sendHtmlEmail(to, subject, content);
    }

    private void sendHtmlEmail(String to, String subject, String content) {
        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            logger.warn("Brevo API Key is missing. Skipping email to {}. Link generated: \n{}", to, content);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            Map<String, Object> body = Map.of(
                    "sender", Map.of("name", "I-SPICE", "email", fromEmail != null ? fromEmail : "ussreedev@gmail.com"),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", content);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject("https://api.brevo.com/v3/smtp/email", request, String.class);

            logger.info("Email sent successfully via BREVO API to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email via BREVO API to {}. Exception: {}", to, e.getMessage(), e);
            logger.warn("Fallback link generated: \n{}", content);
        }
    }
}
