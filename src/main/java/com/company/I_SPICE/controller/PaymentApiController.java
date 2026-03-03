package com.company.I_SPICE.controller;

import com.company.I_SPICE.model.Cart;
import com.company.I_SPICE.model.User;
import com.company.I_SPICE.services.CartService;
import com.company.I_SPICE.services.RazorpayService;
import com.company.I_SPICE.services.UserService;
import com.razorpay.Order;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
public class PaymentApiController {

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    /**
     * Endpoint to create a Razorpay order before opening the checkout popup.
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("error", "User not logged in");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = userService.findByEmail(principal.getName())
                    .orElseGet(() -> userService.findByUsername(principal.getName())
                            .orElseThrow(() -> new RuntimeException("User not found")));

            // Calculate the total cart amount to charge
            Cart cart = cartService.getCartDetails(user.getId());
            if (cart == null || cart.isEmpty()) {
                response.put("error", "Cart is empty");
                return ResponseEntity.badRequest().body(response);
            }

            BigDecimal subtotal = cart.getSubtotal();
            BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.18));
            BigDecimal shipping = cart.getShipping();
            BigDecimal totalAmount = subtotal.add(shipping).add(tax);

            // Create Razorpay Order
            String tempReceiptId = "txn_" + UUID.randomUUID().toString().substring(0, 8);
            Order razorpayOrder = razorpayService.createOrder(totalAmount, tempReceiptId);

            System.out.println("✅ Created Razorpay Order: " + razorpayOrder.get("id"));

            // Return the necessary details to the frontend
            response.put("keyId", razorpayService.getKeyId());
            response.put("orderId", razorpayOrder.get("id"));
            response.put("amount", razorpayOrder.get("amount")); // in paise
            response.put("currency", razorpayOrder.get("currency"));

            // Helpful data for the checkout form prefill
            Map<String, String> prefill = new HashMap<>();
            prefill.put("name", user.getFirstName() + " " + user.getLastName());
            prefill.put("email", user.getEmail());
            prefill.put("contact", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
            response.put("prefill", prefill);

            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            System.err.println("❌ Razorpay exception while creating order: " + e.getMessage());
            response.put("error", "Failed to initialize payment gateway.");
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            System.err.println("❌ Exception while creating order: " + e.getMessage());
            response.put("error", "An error occurred during checkout setup.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint to verify the payment signature after successful completion on the
     * frontend.
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();

        String razorpayOrderId = payload.get("razorpay_order_id");
        String razorpayPaymentId = payload.get("razorpay_payment_id");
        String razorpaySignature = payload.get("razorpay_signature");

        if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
            response.put("success", false);
            response.put("error", "Missing required verification parameters.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            boolean isValid = razorpayService.verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);

            if (isValid) {
                System.out.println("✅ Payment Signature Verified for Payment ID: " + razorpayPaymentId);
                response.put("success", true);
                response.put("message", "Payment verified successfully");
            } else {
                System.err.println("❌ Payment Signature Verification FAILED for Payment ID: " + razorpayPaymentId);
                response.put("success", false);
                response.put("error", "Payment verification failed. Invalid signature.");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Exception while verifying payment: " + e.getMessage());
            response.put("success", false);
            response.put("error", "An error occurred during payment verification.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint to create a Razorpay order for a subscription plan.
     */
    @PostMapping("/subscription/create-order")
    public ResponseEntity<Map<String, Object>> createSubscriptionOrder(@RequestBody Map<String, Object> payload,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("error", "User not logged in");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = userService.findByEmail(principal.getName())
                    .orElseGet(() -> userService.findByUsername(principal.getName())
                            .orElseThrow(() -> new RuntimeException("User not found")));

            String plan = (String) payload.get("plan");
            boolean isAnnual = (Boolean) payload.get("annual");

            BigDecimal amount;

            if ("STARTER".equalsIgnoreCase(plan)) {
                amount = isAnnual ? BigDecimal.valueOf(2868) : BigDecimal.valueOf(299);
            } else if ("PRO".equalsIgnoreCase(plan)) {
                amount = isAnnual ? BigDecimal.valueOf(5750) : BigDecimal.valueOf(599);
            } else if ("ELITE".equalsIgnoreCase(plan)) {
                amount = isAnnual ? BigDecimal.valueOf(9590) : BigDecimal.valueOf(999);
            } else {
                response.put("error", "Invalid subscription plan");
                return ResponseEntity.badRequest().body(response);
            }

            // Create Razorpay Order
            String tempReceiptId = "sub_" + UUID.randomUUID().toString().substring(0, 8);
            Order razorpayOrder = razorpayService.createOrder(amount, tempReceiptId);

            System.out.println("✅ Created Razorpay Subscription Order: " + razorpayOrder.get("id"));

            // Return the necessary details to the frontend
            response.put("keyId", razorpayService.getKeyId());
            response.put("orderId", razorpayOrder.get("id"));
            response.put("amount", razorpayOrder.get("amount")); // in paise
            response.put("currency", razorpayOrder.get("currency"));

            // Helpful data for the checkout form prefill
            Map<String, String> prefill = new HashMap<>();
            prefill.put("name", user.getFirstName() + " " + user.getLastName());
            prefill.put("email", user.getEmail());
            prefill.put("contact", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
            response.put("prefill", prefill);

            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            System.err.println("❌ Razorpay exception while creating subscription order: " + e.getMessage());
            response.put("error", "Failed to initialize payment gateway.");
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            System.err.println("❌ Exception while creating subscription order: " + e.getMessage());
            response.put("error", "An error occurred during subscription checkout setup.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint to verify the subscription payment signature and update user
     * profile.
     */
    @PostMapping("/subscription/verify")
    public ResponseEntity<Map<String, Object>> verifySubscriptionPayment(@RequestBody Map<String, Object> payload,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("error", "User not logged in");
            return ResponseEntity.status(401).body(response);
        }

        String razorpayOrderId = (String) payload.get("razorpay_order_id");
        String razorpayPaymentId = (String) payload.get("razorpay_payment_id");
        String razorpaySignature = (String) payload.get("razorpay_signature");
        String plan = (String) payload.get("plan");
        Boolean isAnnual = (Boolean) payload.get("annual");

        if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null || plan == null) {
            response.put("success", false);
            response.put("error", "Missing required verification parameters.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            boolean isValid = razorpayService.verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);

            if (isValid) {
                System.out.println("✅ Subscription Signature Verified for Payment ID: " + razorpayPaymentId);

                User user = userService.findByEmail(principal.getName())
                        .orElseGet(() -> userService.findByUsername(principal.getName())
                                .orElseThrow(() -> new RuntimeException("User not found")));

                user.setSubscriptionPlan(plan.toUpperCase());
                user.setSubscriptionStatus("ACTIVE");

                int monthsToAdd = isAnnual != null && isAnnual ? 12 : 1;
                LocalDateTime newEndDate = LocalDateTime.now().plusMonths(monthsToAdd);

                // If they already have an active subscription, add to it
                if (user.getSubscriptionEndDate() != null
                        && user.getSubscriptionEndDate().isAfter(LocalDateTime.now())) {
                    newEndDate = user.getSubscriptionEndDate().plusMonths(monthsToAdd);
                }

                user.setSubscriptionEndDate(newEndDate);
                userService.updateUser(user);

                response.put("success", true);
                response.put("message", "Subscription activated successfully");

                Map<String, Object> subDetails = new HashMap<>();
                subDetails.put("plan", user.getSubscriptionPlan());
                subDetails.put("status", user.getSubscriptionStatus());
                subDetails.put("endDate", user.getSubscriptionEndDate().toString());
                response.put("subscription", subDetails);

            } else {
                System.err.println("❌ Subscription Signature Verification FAILED for Payment ID: " + razorpayPaymentId);
                response.put("success", false);
                response.put("error", "Payment verification failed. Invalid signature.");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Exception while verifying subscription payment: " + e.getMessage());
            response.put("success", false);
            response.put("error", "An error occurred during payment verification.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
