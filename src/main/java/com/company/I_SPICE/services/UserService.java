package com.company.I_SPICE.services;

import com.company.I_SPICE.model.User;
import com.company.I_SPICE.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(User user) {
        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set default values for Supabase compatibility
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        user.setUpdatedAt(LocalDateTime.now());

        // Ensure fullName is set (important for database constraint)
        if (user.getFullName() == null || user.getFullName().isEmpty()) {
            user.setFullName(user.getFirstName() + " " + user.getLastName());
        }

        // Generate verification token (optional for now)
        if (user.getVerificationToken() == null) {
            user.setVerificationToken(UUID.randomUUID().toString());
        }

        // Default values
        if (user.getLoyaltyPoints() == 0) {
            user.setLoyaltyPoints(50); // Welcome bonus
        }

        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }

        // Ensure other defaults are set
        if (!user.isEmailVerified()) {
            user.setEmailVerified(false);
        }

        if (!user.isTermsAccepted()) {
            user.setTermsAccepted(false);
        }

        if (!user.isNewsletterSubscription()) {
            user.setNewsletterSubscription(false);
        }

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public int getOrderCount(Long userId) {
        // For testing, return 5
        return 5;
    }

    @Transactional
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());

        // Ensure fullName is updated if firstName or lastName changed
        if (user.getFirstName() != null && user.getLastName() != null) {
            user.setFullName(user.getFirstName() + " " + user.getLastName());
        }

        return userRepository.save(user);
    }

    @Transactional
    public User updateUserProfile(Long id, User updatedUser) {
        Optional<User> existingUserOpt = userRepository.findById(id);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            // Update fields that can be modified
            if (updatedUser.getFirstName() != null) {
                existingUser.setFirstName(updatedUser.getFirstName());
            }
            if (updatedUser.getLastName() != null) {
                existingUser.setLastName(updatedUser.getLastName());
            }
            if (updatedUser.getPhoneNumber() != null) {
                existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
            }

            existingUser.setNewsletterSubscription(updatedUser.isNewsletterSubscription());

            // Update fullName
            if (existingUser.getFirstName() != null && existingUser.getLastName() != null) {
                existingUser.setFullName(existingUser.getFirstName() + " " + existingUser.getLastName());
            }

            existingUser.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(existingUser);
        }
        return null;
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserFromPrincipal(java.security.Principal principal) {
        if (principal == null)
            return Optional.empty();

        String identifier = principal.getName();

        if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
            org.springframework.security.oauth2.core.user.OAuth2User oauth2User = ((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) principal)
                    .getPrincipal();
            if (oauth2User.getAttribute("email") != null) {
                identifier = oauth2User.getAttribute("email");
            }
        }

        Optional<User> userOpt = findByEmail(identifier);
        if (userOpt.isEmpty()) {
            userOpt = findByUsername(principal.getName());
        }
        return userOpt;
    }

    // FIX: Add getAllUsers method that returns List<User>
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Keep the existing method for backward compatibility
    public Iterable<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public boolean verifyEmail(String token) {
        Optional<User> userOpt = userRepository.findByVerificationToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEmailVerified(true);
            user.setVerificationToken(null); // Clear token after verification
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Transactional
    public User updatePassword(Long userId, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        }
        return null;
    }

    @Transactional
    public User updateLoyaltyPoints(Long userId, int pointsToAdd) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLoyaltyPoints(user.getLoyaltyPoints() + pointsToAdd);
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        }
        return null;
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Transactional
    public void markEmailAsVerified(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEmailVerified(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    // ─── PASSWORD RESET ──────────────────────────────────────────────────────

    /**
     * Generates a reset token for the given email (if user exists) and saves it
     * with a 1-hour expiry. Returns the token, or empty if email not found.
     */
    @Transactional
    public Optional<String> generatePasswordResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordExpiry(LocalDateTime.now().plusHours(1));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return Optional.of(token);
    }

    /**
     * Validates the given reset token. Returns the User if the token is valid
     * and not expired, otherwise returns empty.
     */
    public Optional<User> validateResetToken(String token) {
        Optional<User> userOpt = userRepository.findByResetPasswordToken(token);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        if (user.getResetPasswordExpiry() == null ||
                LocalDateTime.now().isAfter(user.getResetPasswordExpiry())) {
            return Optional.empty(); // token expired
        }
        return userOpt;
    }

    /**
     * Resets the password for the user with the given token, then clears it.
     * Returns true if successful, false if token invalid/expired.
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<User> userOpt = validateResetToken(token);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return true;
    }
}
