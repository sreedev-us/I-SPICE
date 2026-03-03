package com.company.I_SPICE.controller;

import com.company.I_SPICE.model.User;
import com.company.I_SPICE.services.UserService;
import com.company.I_SPICE.services.EmailService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    private final UserService userService;
    private final EmailService emailService;

    public UserController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    // Show registration form
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "register";
    }

    // Process registration form
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam(value = "terms", required = false) String terms,
            @RequestParam(value = "newsletter", required = false) boolean newsletter,
            RedirectAttributes redirectAttributes) {

        // Check if terms are accepted
        if (terms == null) {
            bindingResult.rejectValue("termsAccepted", "error.user",
                    "You must agree to the Terms of Service and Privacy Policy");
            user.setTermsAccepted(false);
        } else {
            user.setTermsAccepted(true);
        }

        // Check password confirmation
        if (!user.getPassword().equals(confirmPassword)) {
            bindingResult.rejectValue("password", "error.user",
                    "Passwords do not match");
        }

        // Password strength validation
        if (user.getPassword() != null && user.getPassword().length() < 8) {
            bindingResult.rejectValue("password", "error.user",
                    "Password must be at least 8 characters long");
        }

        // Check if email already exists
        if (user.getEmail() != null && userService.emailExists(user.getEmail())) {
            bindingResult.rejectValue("email", "error.user",
                    "Email already registered. Please use a different email.");
        }

        // Check if username already exists
        if (user.getUsername() != null && userService.usernameExists(user.getUsername())) {
            bindingResult.rejectValue("username", "error.user",
                    "Username already taken. Please choose a different username.");
        }

        // If there are validation errors, return to registration form
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/register";
        }

        // Set newsletter preference
        user.setNewsletterSubscription(newsletter);
        user.setRole("USER"); // Default role

        // Ensure fullName is set for new registrations
        if (user.getFullName() == null || user.getFullName().isEmpty()) {
            user.setFullName(user.getFirstName() + " " + user.getLastName());
        }

        try {
            // Register user
            User registeredUser = userService.registerUser(user);

            // Send verification email
            emailService.sendVerificationEmail(registeredUser.getEmail(), registeredUser.getVerificationToken(),
                    registeredUser.getFirstName());

            redirectAttributes.addFlashAttribute("success",
                    "Registration successful! Please check your email to verify your account before logging in.");
            redirectAttributes.addAttribute("registered", true);

            return "redirect:/login?registered=true";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Registration failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/register";
        }
    }

    // Check username availability (AJAX endpoint)
    @GetMapping("/api/check-username")
    @ResponseBody
    public ApiResponse checkUsername(@RequestParam String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return new ApiResponse(false, "Username cannot be empty");
            }
            boolean exists = userService.usernameExists(username.trim());
            return new ApiResponse(!exists, exists ? "Username already taken" : "Username available");
        } catch (Exception e) {
            return new ApiResponse(false, "Error checking username: " + e.getMessage());
        }
    }

    // Check email availability (AJAX endpoint)
    @GetMapping("/api/check-email")
    @ResponseBody
    public ApiResponse checkEmail(@RequestParam String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return new ApiResponse(false, "Email cannot be empty");
            }
            boolean exists = userService.emailExists(email.trim());
            return new ApiResponse(!exists, exists ? "Email already registered" : "Email available");
        } catch (Exception e) {
            return new ApiResponse(false, "Error checking email: " + e.getMessage());
        }
    }

    // Verify email endpoint (for email verification links)
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, RedirectAttributes redirectAttributes) {
        try {
            boolean verified = userService.verifyEmail(token);
            if (verified) {
                redirectAttributes.addFlashAttribute("success", "Email verified successfully!");
                return "redirect:/login?verified=true";
            } else {
                redirectAttributes.addFlashAttribute("error", "Invalid or expired verification token");
                return "redirect:/login";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error verifying email: " + e.getMessage());
            return "redirect:/login";
        }
    }

    // Simple response class for AJAX calls
    public static class ApiResponse {
        private boolean success;
        private String message;

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        // Getters and setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}