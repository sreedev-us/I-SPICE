package com.company.I_SPICE.controller;

import com.company.I_SPICE.model.*;
import com.company.I_SPICE.services.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;

/**
 * Handles all secondary pages:
 * GET /orders â€“ Orders list
 * GET /orders/{id} â€“ Order detail (optional future)
 * POST /api/orders/{id}/cancel â€“ Cancel an order
 * GET /wishlist â€“ Wishlist page
 * POST /api/wishlist/add â€“ Add to wishlist (JSON)
 * POST /api/wishlist/remove â€“ Remove from wishlist (JSON)
 * GET /api/wishlist/check â€“ Check if in wishlist (JSON)
 * GET /subscriptions â€“ Subscriptions / plans page
 * GET /profile â€“ My profile page
 * POST /api/profile/update â€“ Save profile changes (form)
 * GET /support â€“ Support / contact page
 * POST /api/support/submit â€“ Submit a support ticket (JSON)
 */
@Controller
public class PageController {

    private final UserService userService;
    private final OrderService orderService;
    private final CartService cartService;
    private final WishlistService wishlistService;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionPlanService subscriptionPlanService;

    public PageController(UserService userService,
            OrderService orderService,
            CartService cartService,
            WishlistService wishlistService,
            PasswordEncoder passwordEncoder,
            SubscriptionPlanService subscriptionPlanService) {
        this.userService = userService;
        this.orderService = orderService;
        this.cartService = cartService;
        this.wishlistService = wishlistService;
        this.passwordEncoder = passwordEncoder;
        this.subscriptionPlanService = subscriptionPlanService;
    }

    // â”€â”€â”€ Shared helper
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private User getUser(Principal principal) {
        return userService.getUserFromPrincipal(principal).orElse(null);
    }

    private void addCommonModel(Model model, User user) {
        model.addAttribute("user", user);
        if (user != null) {
            model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
        } else {
            model.addAttribute("cartCount", 0);
        }
    }

    private void addSubscriptionModel(Model model, User user) {
        var benefits = subscriptionPlanService.getBenefitsForUser(user);
        model.addAttribute("subscriptionBenefits", benefits);

        if (user != null && user.hasActiveSubscription()) {
            subscriptionPlanService.getPlanByCode(user.getSubscriptionPlan())
                    .ifPresent(plan -> model.addAttribute("activeSubscriptionPlan", plan));

            List<String> activeBenefitSummary = new ArrayList<>();
            if (benefits.productDiscountPercent() > 0) {
                activeBenefitSummary.add(benefits.productDiscountPercent() + "% extra member discount on products");
            }
            if (benefits.alwaysFreeShipping()) {
                activeBenefitSummary.add("Always free shipping on all eligible orders");
            } else if (benefits.freeShippingThreshold() != null) {
                activeBenefitSummary.add("Free shipping from Rs " + benefits.freeShippingThreshold().intValue());
            }
            if (benefits.loyaltyMultiplier() > 1) {
                activeBenefitSummary.add(benefits.loyaltyMultiplier() + "x loyalty points on orders");
            }
            activeBenefitSummary.add("Support priority: " + benefits.supportPriority());
            model.addAttribute("activeBenefitSummary", activeBenefitSummary);
        } else {
            model.addAttribute("activeBenefitSummary", List.of());
        }
    }

    // â”€â”€â”€ ORDERS
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/orders")
    public String ordersPage(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        User user = getUser(principal);
        if (user == null)
            return "redirect:/login";

        addCommonModel(model, user);

        try {
            List<Order> orders = orderService.getUserOrders(user.getId());
            model.addAttribute("orders", orders);
        } catch (Exception e) {
            System.err.println("Error loading orders: " + e.getMessage());
            model.addAttribute("orders", new ArrayList<>());
        }

        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetailPage(@PathVariable Long id, Model model, Principal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null)
            return "redirect:/login";
        User user = getUser(principal);
        if (user == null)
            return "redirect:/login";

        Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty() || !orderOpt.get().getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Order not found");
            return "redirect:/orders";
        }

        addCommonModel(model, user);
        model.addAttribute("order", orderOpt.get());
        return "order-detail";
    }

    @PostMapping("/api/orders/{id}/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable Long id, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        if (principal == null) {
            response.put("success", false);
            response.put("message", "Not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = getUser(principal);
            Order order = orderService.getOrderById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Security: only the owner can cancel
            if (!order.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(403).body(response);
            }

            orderService.cancelOrder(id);
            response.put("success", true);
            response.put("message", "Order cancelled successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    private Map<Long, BigDecimal> buildEffectivePriceMap(List<Product> products, User user) {
        Map<Long, BigDecimal> prices = new HashMap<>();
        for (Product product : products) {
            prices.put(product.getId(), subscriptionPlanService.getEffectiveProductPrice(user, product));
        }
        return prices;
    }

    private Map<Long, BigDecimal> buildBasePriceMap(List<Product> products) {
        Map<Long, BigDecimal> prices = new HashMap<>();
        for (Product product : products) {
            prices.put(product.getId(), subscriptionPlanService.getBaseProductPrice(product));
        }
        return prices;
    }

    private Map<Long, Boolean> buildSubscriptionSavingsMap(List<Product> products, User user) {
        Map<Long, Boolean> savings = new HashMap<>();
        for (Product product : products) {
            savings.put(product.getId(), subscriptionPlanService.hasSubscriptionSavings(user, product));
        }
        return savings;
    }

    @PostMapping("/api/orders/{id}/reorder")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reorderOrder(@PathVariable Long id, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        if (principal == null) {
            response.put("success", false);
            response.put("message", "Not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = getUser(principal);
            Order order = orderService.getOrderById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Security: only the owner can cancel
            if (!order.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(403).body(response);
            }

            for (OrderItem item : order.getOrderItems()) {
                cartService.addToCart(user.getId(), item.getProduct().getId(), item.getQuantity());
            }

            response.put("success", true);
            response.put("message", "Order items added to cart successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // â”€â”€â”€ WISHLIST
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/wishlist")
    public String wishlistPage(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        User user = getUser(principal);
        if (user == null)
            return "redirect:/login";

        addCommonModel(model, user);

        try {
            List<Product> wishlist = wishlistService.getWishlistProducts(user.getId());
            model.addAttribute("wishlist", wishlist);
            model.addAttribute("wishlistEffectivePrices", buildEffectivePriceMap(wishlist, user));
            model.addAttribute("wishlistBasePrices", buildBasePriceMap(wishlist));
            model.addAttribute("wishlistHasSubscriptionSavings", buildSubscriptionSavingsMap(wishlist, user));
            model.addAttribute("subscriptionProductDiscountPercent",
                    subscriptionPlanService.getBenefitsForUser(user).productDiscountPercent());
        } catch (Exception e) {
            System.err.println("Error loading wishlist: " + e.getMessage());
            model.addAttribute("wishlist", new ArrayList<>());
        }

        return "wishlist";
    }

    @PostMapping("/api/wishlist/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToWishlist(
            @RequestBody Map<String, Object> body,
            Principal principal) {

        Map<String, Object> response = new HashMap<>();
        if (principal == null) {
            response.put("success", false);
            response.put("message", "Not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = getUser(principal);
            Long productId = Long.valueOf(body.get("productId").toString());
            boolean added = wishlistService.addToWishlist(user, productId);
            response.put("success", true);
            response.put("added", added);
            response.put("message", added ? "Added to wishlist" : "Already in wishlist");
            response.put("wishlistCount", wishlistService.getWishlistCount(user.getId()));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/wishlist/remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromWishlist(
            @RequestBody Map<String, Object> body,
            Principal principal) {

        Map<String, Object> response = new HashMap<>();
        if (principal == null) {
            response.put("success", false);
            response.put("message", "Not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = getUser(principal);
            Long productId = Long.valueOf(body.get("productId").toString());
            wishlistService.removeFromWishlist(user.getId(), productId);
            response.put("success", true);
            response.put("message", "Removed from wishlist");
            response.put("wishlistCount", wishlistService.getWishlistCount(user.getId()));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/wishlist/check")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkWishlist(
            @RequestParam Long productId,
            Principal principal) {

        Map<String, Object> response = new HashMap<>();
        if (principal == null) {
            response.put("inWishlist", false);
            return ResponseEntity.ok(response);
        }

        User user = getUser(principal);
        boolean inWishlist = (user != null) && wishlistService.isInWishlist(user.getId(), productId);
        response.put("inWishlist", inWishlist);
        return ResponseEntity.ok(response);
    }

    // â”€â”€â”€ SUBSCRIPTIONS
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/subscriptions")
    public String subscriptionsPage(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        User user = getUser(principal);
        if (user == null)
            return "redirect:/login";

        addCommonModel(model, user);
        model.addAttribute("plans", subscriptionPlanService.getActivePlans());
        addSubscriptionModel(model, user);
        return "subscriptions";
    }

    // â”€â”€â”€ PROFILE
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/profile")
    public String profilePage(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        User user = getUser(principal);
        if (user == null)
            return "redirect:/login";

        addCommonModel(model, user);
        addSubscriptionModel(model, user);

        // Order stats for profile sidebar
        try {
            long orderCount = orderService.getOrderCount(user.getId());
            model.addAttribute("orderCount", orderCount);
        } catch (Exception e) {
            model.addAttribute("orderCount", 0);
        }

        // Flash messages support (passed as redirect attributes)
        return "profile";
    }

    @PostMapping("/api/profile/update")
    public String updateProfile(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String productInterest,
            @RequestParam(required = false, defaultValue = "false") boolean newsletterSubscription,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null)
            return "redirect:/login";

        try {
            User user = getUser(principal);
            if (user == null)
                return "redirect:/login";

            // Update only the editable fields
            if (firstName != null && !firstName.isBlank())
                user.setFirstName(firstName.trim());
            if (lastName != null && !lastName.isBlank())
                user.setLastName(lastName.trim());
            if (phoneNumber != null)
                user.setPhoneNumber(phoneNumber.trim());

            user.setNewsletterSubscription(newsletterSubscription);

            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not update profile: " + e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/api/profile/change-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody Map<String, String> body,
            Principal principal) {

        Map<String, Object> response = new HashMap<>();
        if (principal == null) {
            response.put("success", false);
            response.put("message", "Not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = getUser(principal);
            String currentPassword = body.get("currentPassword");
            String newPassword = body.get("newPassword");

            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                response.put("success", false);
                response.put("message", "Current password is incorrect");
                return ResponseEntity.ok(response);
            }

            if (newPassword == null || newPassword.length() < 8) {
                response.put("success", false);
                response.put("message", "New password must be at least 8 characters");
                return ResponseEntity.ok(response);
            }

            userService.updatePassword(user.getId(), newPassword);
            response.put("success", true);
            response.put("message", "Password updated successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // â”€â”€â”€ SUPPORT
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    

    @PostMapping("/api/support/submit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitSupportTicket(
            @RequestBody Map<String, String> body,
            Principal principal) {

        Map<String, Object> response = new HashMap<>();
        try {
            String subject = body.getOrDefault("subject", "").trim();
            String message = body.getOrDefault("message", "").trim();
            String category = body.getOrDefault("category", "General").trim();

            if (subject.isEmpty() || message.isEmpty()) {
                response.put("success", false);
                response.put("message", "Subject and message are required");
                return ResponseEntity.ok(response);
            }

            // In a real application, this would create a support ticket in the DB
            // or send an email. For now, we just log it and return success.
            String userInfo = (principal != null) ? principal.getName() : "anonymous";
            System.out.println("ðŸ“§ Support ticket from " + userInfo +
                    " | Category: " + category +
                    " | Subject: " + subject);

            response.put("success", true);
            response.put("ticketId", "TKT-" + System.currentTimeMillis());
            response.put("message", "Your request has been received. We'll get back to you within 24 hours.");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to submit ticket: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
}

