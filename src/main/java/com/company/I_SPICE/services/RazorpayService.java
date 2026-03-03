package com.company.I_SPICE.services;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private RazorpayClient client;

    @PostConstruct
    public void init() {
        try {
            // Initialize the Razorpay client once when the service is created
            this.client = new RazorpayClient(keyId, keySecret);
            System.out.println("✅ RazorpayClient initialized successfully.");
        } catch (RazorpayException e) {
            System.err.println("❌ Failed to initialize RazorpayClient: " + e.getMessage());
        }
    }

    /**
     * Creates an order in Razorpay.
     *
     * @param amountInRupees The order amount in INR (Rupees).
     * @param receiptId      A unique identifier for the receipt (e.g., cart ID or
     *                       temp order ID).
     * @return The Razorpay Order object containing the generated order_id.
     * @throws RazorpayException If the API call fails.
     */
    public Order createOrder(BigDecimal amountInRupees, String receiptId) throws RazorpayException {
        // Razorpay expects the amount in the smallest subunit (paise).
        // Multiply by 100 to convert Rupees to Paise.
        int amountInPaise = amountInRupees.multiply(new BigDecimal("100")).intValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receiptId);
        // Optional: Auto capture payments (useful for immediate confirmation)
        orderRequest.put("payment_capture", 1);

        return client.orders.create(orderRequest);
    }

    /**
     * Verifies the cryptographic signature returned by Razorpay after a successful
     * payment.
     * This ensures the payment data was not tampered with on the client side.
     *
     * @param orderId   The Razorpay order_id.
     * @param paymentId The Razorpay payment_id.
     * @param signature The Razorpay signature provided in the callback.
     * @return true if the signature is valid, false otherwise.
     */
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            // The Utils.verifyPaymentSignature uses the secret key to regenerate
            // the signature and compares it against the provided signature.
            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            System.err.println("❌ Signature verification failed: " + e.getMessage());
            return false;
        }
    }

    public String getKeyId() {
        return keyId;
    }
}
