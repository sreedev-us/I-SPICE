package com.company.I_SPICE.controller;

import com.company.I_SPICE.model.User;
import com.company.I_SPICE.services.EmailService;
import com.company.I_SPICE.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Handles forgot-password and reset-password flows.
 *
 * GET /forgot-password – show email form
 * POST /forgot-password – send reset email
 * GET /reset-password?token= – show new-password form (validates token)
 * POST /reset-password – apply new password
 */
@Controller
public class ForgotPasswordController {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordController.class);

    private final UserService userService;
    private final EmailService emailService;

    public ForgotPasswordController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    // ─── Forgot Password ─────────────────────────────────────────────────────

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String email,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<String> tokenOpt = userService.generatePasswordResetToken(email.trim().toLowerCase());

            // Always show success message to prevent email enumeration
            if (tokenOpt.isPresent()) {
                userService.findByEmail(email.trim().toLowerCase()).ifPresent(user -> {
                    String name = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
                    emailService.sendPasswordResetEmail(user.getEmail(), tokenOpt.get(), name);
                    log.info("Password reset email sent to {}", user.getEmail());
                });
            } else {
                log.info("Password reset requested for unknown email: {}", email);
            }

            redirectAttributes.addFlashAttribute("success",
                    "If an account with that email exists, a reset link has been sent. Check your inbox.");
        } catch (Exception e) {
            log.error("Error processing forgot password request", e);
            redirectAttributes.addFlashAttribute("error",
                    "Something went wrong. Please try again.");
        }

        return "redirect:/forgot-password";
    }

    // ─── Reset Password ───────────────────────────────────────────────────────

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("error", "Invalid or missing reset link. Please request a new one.");
            return "reset-password";
        }

        Optional<User> userOpt = userService.validateResetToken(token);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "This reset link has expired or already been used. Please request a new one.");
            return "reset-password";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam String token,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        // Validate passwords match
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            redirectAttributes.addFlashAttribute("token", token);
            return "redirect:/reset-password?token=" + token;
        }

        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters.");
            redirectAttributes.addFlashAttribute("token", token);
            return "redirect:/reset-password?token=" + token;
        }

        boolean success = userService.resetPassword(token, password);
        if (success) {
            redirectAttributes.addFlashAttribute("success",
                    "Password reset successfully! You can now log in with your new password.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "This reset link has expired or already been used. Please request a new one.");
            return "redirect:/forgot-password";
        }
    }
}
