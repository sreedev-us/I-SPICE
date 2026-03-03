package com.company.I_SPICE.services;

import com.company.I_SPICE.model.*;
import com.company.I_SPICE.repository.OrderRepository;
import com.company.I_SPICE.repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final UserService userService;
    private final ProductService productService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartService cartService,
                        UserService userService,
                        ProductService productService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartService = cartService;
        this.userService = userService;
        this.productService = productService;
    }

    // Create order from cart
    @Transactional
    public Order createOrderFromCart(Long userId, String shippingAddress,
                                     String billingAddress, String paymentMethod) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Cart cart = cartService.getCartDetails(userId);
        if (cart == null || cart.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Check product stock before creating order
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName() +
                        ". Available: " + product.getStock() +
                        ", Requested: " + cartItem.getQuantity());
            }
        }

        // Create order from cart
        Order order = new Order(cart);
        order.setShippingAddress(shippingAddress);
        order.setBillingAddress(billingAddress != null ? billingAddress : shippingAddress);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("PAID");

        // Save order first to get ID
        Order savedOrder = orderRepository.save(order);

        // Convert cart items to order items and reduce stock
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem(savedOrder, cartItem.getProduct(),
                    cartItem.getQuantity(), cartItem.getUnitPrice());
            orderItemRepository.save(orderItem);

            // Reduce product stock
            Product product = cartItem.getProduct();
            product.setStock(product.getStock() - cartItem.getQuantity());
            productService.saveProduct(product);
        }

        // Recalculate and save totals
        savedOrder.calculateTotals();
        savedOrder = orderRepository.save(savedOrder);

        // Add loyalty points (10 points per ₹100 spent)
        BigDecimal totalAmount = savedOrder.getTotal();
        if (totalAmount != null) {
            int loyaltyPoints = totalAmount.divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.DOWN)
                    .intValue() * 10;
            userService.updateLoyaltyPoints(userId, loyaltyPoints);
        }

        // Clear cart after successful order creation
        cartService.clearCart(userId);

        return savedOrder;
    }

    // Get all orders for a user
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    // Get recent orders with limit
    public List<Order> getRecentOrders(Long userId, int limit) {
        List<Order> allOrders = getUserOrders(userId);
        return allOrders.stream().limit(limit).toList();
    }

    // Get order count for a user
    public Integer getOrderCount(Long userId) {
        return (int) orderRepository.countByUserId(userId);
    }

    // Get order by ID
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    // Update order status
    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.setStatus(status);

        // If order is delivered, set delivered timestamp
        if (status == Order.OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        return orderRepository.save(order);
    }

    // Cancel order
    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Only allow cancellation if order is pending or processing
        if (order.getStatus() != Order.OrderStatus.PENDING &&
                order.getStatus() != Order.OrderStatus.PROCESSING) {
            throw new RuntimeException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        // Restore product stock
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            product.setStock(product.getStock() + orderItem.getQuantity());
            productService.saveProduct(product);
        }

        // Update order status
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setPaymentStatus("REFUNDED");

        return orderRepository.save(order);
    }
}