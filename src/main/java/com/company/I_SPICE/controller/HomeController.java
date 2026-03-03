package com.company.I_SPICE.controller;

import com.company.I_SPICE.model.*;
import com.company.I_SPICE.services.*;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.math.BigDecimal;
import java.util.*;

@Controller
public class HomeController {

    private final UserService userService;
    private final OrderService orderService;
    private final ProductService productService;
    private final CartService cartService;

    public HomeController(UserService userService,
            OrderService orderService,
            ProductService productService,
            CartService cartService) {
        this.userService = userService;
        this.orderService = orderService;
        this.productService = productService;
        this.cartService = cartService;
    }

    // ==================== DASHBOARD & PAGES ====================

    // ==================== DASHBOARD & PAGES ====================

    @GetMapping({ "/", "/home", "/dashboard" })
    public String home(Model model, Principal principal, HttpServletRequest request) {
        System.out.println("Ã°Å¸ÂÂ  Dashboard accessed by: " + (principal != null ? principal.getName() : "anonymous"));

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            // Get user
            User user = userService.getUserFromPrincipal(principal)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("Ã¢Å“â€¦ User found: " + user.getUsername() + " (ID: " + user.getId() + ")");

            // Add user data to model
            model.addAttribute("user", user);
            model.addAttribute("username", user.getUsername());

            // Get cart count
            int cartCount = cartService.getCartItemCount(user.getId());
            model.addAttribute("cartCount", cartCount);
            System.out.println("Ã°Å¸â€ºâ€™ Cart count: " + cartCount);

            // Add dashboard statistics
            Integer orderCount = orderService.getOrderCount(user.getId());
            model.addAttribute("orderCount", orderCount);
            model.addAttribute("loyaltyPoints", user.getLoyaltyPoints());

            // Add featured products from database
            List<Product> featuredProducts = productService.getFeaturedProducts();

            // Calculate discounted prices
            if (featuredProducts != null) {
                featuredProducts.forEach(product -> {
                    if (product.getDiscount() != null && product.getDiscount() > 0) {
                        double discountedPrice = product.getPrice().doubleValue() * (1 - product.getDiscount() / 100.0);
                        product.setDiscountedPrice(Math.round(discountedPrice * 100.0) / 100.0);
                    } else {
                        product.setDiscountedPrice(product.getPrice().doubleValue());
                    }
                });
            }

            model.addAttribute("featuredProducts", featuredProducts);
            System.out
                    .println("Ã°Å¸â€œÂ¦ Featured products count: " + (featuredProducts != null ? featuredProducts.size() : 0));

            // Add sustainability calculations
            model.addAttribute("plasticSaved", calculatePlasticSaved(orderCount));
            model.addAttribute("waterSaved", calculateWaterSaved(orderCount));
            model.addAttribute("carbonSaved", calculateCarbonSaved(orderCount));

            // Add recent orders
            try {
                List<Order> recentOrders = orderService.getRecentOrders(user.getId(), 5);
                model.addAttribute("recentOrders", recentOrders);
                System.out.println("Ã°Å¸â€œâ€¹ Recent orders count: " + recentOrders.size());
            } catch (Exception e) {
                System.out.println("Ã¢Å¡Â Ã¯Â¸Â Could not load recent orders: " + e.getMessage());
                model.addAttribute("recentOrders", new ArrayList<>());
            }

            // CSRF token handling
            Object csrfAttribute = request.getAttribute("_csrf");
            if (csrfAttribute == null) {
                csrfAttribute = request.getAttribute(CsrfToken.class.getName());
            }

            if (csrfAttribute instanceof CsrfToken) {
                model.addAttribute("_csrf", csrfAttribute);
                System.out.println("Ã¢Å“â€¦ CSRF token found and added to model");
            } else {
                Map<String, String> dummyCsrf = new HashMap<>();
                dummyCsrf.put("parameterName", "_csrf");
                dummyCsrf.put("token", "dummy-csrf-token-" + UUID.randomUUID().toString());
                model.addAttribute("_csrf", dummyCsrf);
                System.out.println("Ã¢Å¡Â Ã¯Â¸Â Created dummy CSRF token for development");
            }

        } catch (Exception e) {
            System.out.println("Ã¢ÂÅ’ ERROR loading dashboard: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("orderCount", 0);
            model.addAttribute("loyaltyPoints", 0);
            model.addAttribute("featuredProducts", new ArrayList<>());
            model.addAttribute("plasticSaved", 0.0);
            model.addAttribute("waterSaved", 0.0);
            model.addAttribute("carbonSaved", 0.0);
            model.addAttribute("recentOrders", new ArrayList<>());
            model.addAttribute("cartCount", 0);
            model.addAttribute("error", "Failed to load dashboard data: " + e.getMessage());

            Map<String, String> dummyCsrf = new HashMap<>();
            dummyCsrf.put("parameterName", "_csrf");
            dummyCsrf.put("token", "error-csrf-token");
            model.addAttribute("_csrf", dummyCsrf);
        }

        return "home";
    }

    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request, Model model) {
        System.out.println("Ã°Å¸â€Â Login page accessed");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            System.out.println("Ã°Å¸â€˜Â¤ User already authenticated, redirecting to home");
            return "redirect:/home";
        }

        if (request.getParameter("error") != null) {
            model.addAttribute("error", "Invalid username or password");
            System.out.println("Ã¢Å¡Â Ã¯Â¸Â Login error parameter found");
        }

        if (request.getParameter("logout") != null) {
            model.addAttribute("success", "You have been logged out successfully");
            System.out.println("Ã°Å¸â€˜â€¹ Logout parameter found");
        }

        if (request.getParameter("registered") != null) {
            model.addAttribute("success", "Registration successful! Please login.");
            System.out.println("Ã°Å¸Å½â€° Registration success parameter found");
        }

        return "login";
    }

    // /orders is now handled by PageController

    // /profile, /wishlist are now handled by PageController

    // /subscriptions, /support are now handled by PageController

    @GetMapping("/cart")
    public String cart(Model model, Principal principal) {
        System.out.println("Ã°Å¸â€ºâ€™ Cart page accessed");

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            User user = userService.getUserFromPrincipal(principal)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            model.addAttribute("user", user);
            model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));

            Cart cart = cartService.getCartDetails(user.getId());
            model.addAttribute("cart", cart);

            if (cart != null) {
                System.out.println("Ã¢Å“â€¦ Cart loaded with " + cart.getItems().size() + " items");

                if (!cart.isEmpty()) {
                    BigDecimal subtotal = cart.getSubtotal();
                    BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.18));
                    BigDecimal shipping = cart.getShipping();
                    BigDecimal total = subtotal.add(shipping).add(tax);

                    model.addAttribute("subtotal", subtotal);
                    model.addAttribute("tax", tax);
                    model.addAttribute("shipping", shipping);
                    model.addAttribute("total", total);

                    System.out.println("Ã°Å¸â€™Â° Cart totals - Subtotal: " + subtotal + ", Tax: " + tax +
                            ", Shipping: " + shipping + ", Total: " + total);
                } else {
                    System.out.println("Ã°Å¸â€ºâ€™ Cart is empty");
                }
            }

            List<Order> orders = orderService.getUserOrders(user.getId());
            model.addAttribute("hasOrders", !orders.isEmpty());
            System.out.println("Ã°Å¸â€œâ€¹ User has orders: " + !orders.isEmpty());

        } catch (Exception e) {
            System.out.println("Ã¢ÂÅ’ ERROR loading cart: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Failed to load cart: " + e.getMessage());
            model.addAttribute("hasOrders", false);
        }

        return "cart";
    }

    @GetMapping("/checkout")
    public String checkoutPage(Model model, Principal principal) {
        System.out.println("Ã°Å¸â€™Â³ Checkout page accessed");

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            User user = userService.getUserFromPrincipal(principal)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            model.addAttribute("user", user);
            model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));

            Cart cart = cartService.getCartDetails(user.getId());
            if (cart == null || cart.isEmpty()) {
                System.out.println("Ã¢Å¡Â Ã¯Â¸Â Cart is empty, redirecting to cart page");
                return "redirect:/cart";
            }

            model.addAttribute("cart", cart);
            System.out.println("Ã¢Å“â€¦ Cart has " + cart.getItems().size() + " items for checkout");

            BigDecimal subtotal = cart.getSubtotal();
            BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.18));
            BigDecimal shipping = cart.getShipping();
            BigDecimal total = subtotal.add(shipping).add(tax);

            model.addAttribute("subtotal", subtotal);
            model.addAttribute("tax", tax);
            model.addAttribute("shipping", shipping);
            model.addAttribute("total", total);

            // Provide a pre-filled CheckoutForm so Thymeleaf th:object binding works
            CheckoutForm checkoutForm = new CheckoutForm();
            // Pre-fill email from the logged-in user
            checkoutForm.setEmail(user.getEmail());
            if (user.getFirstName() != null)
                checkoutForm.setFirstName(user.getFirstName());
            if (user.getLastName() != null)
                checkoutForm.setLastName(user.getLastName());
            if (user.getPhoneNumber() != null)
                checkoutForm.setPhone(user.getPhoneNumber());
            model.addAttribute("checkoutForm", checkoutForm);

            System.out.println("Ã°Å¸â€™Â° Checkout totals - Subtotal: " + subtotal + ", Tax: " + tax +
                    ", Shipping: " + shipping + ", Total: " + total);

        } catch (Exception e) {
            System.out.println("Ã¢ÂÅ’ ERROR loading checkout: " + e.getMessage());
            model.addAttribute("error", "Failed to load checkout: " + e.getMessage());
            return "redirect:/cart";
        }

        return "checkout";
    }

    @PostMapping("/proceed-to-payment")
    public String proceedToPayment(
            @ModelAttribute("checkoutForm") CheckoutForm checkoutForm,
            HttpSession session) {
        session.setAttribute("pendingCheckoutForm", checkoutForm);
        return "redirect:/payment";
    }

    @GetMapping("/payment")
    public String showPaymentPage(HttpSession session, Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        CheckoutForm form = (CheckoutForm) session.getAttribute("pendingCheckoutForm");
        if (form == null) {
            return "redirect:/checkout";
        }

        User user = userService.findByEmail(principal.getName())
                .orElseGet(() -> userService.findByUsername(principal.getName()).orElse(null));

        if (user != null) {
            model.addAttribute("user", user);
            model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
            Cart cart = cartService.getCartDetails(user.getId());
            model.addAttribute("cart", cart);
        } else {
            model.addAttribute("cartCount", 0);
        }

        return "payment";
    }

    @PostMapping("/checkout")
    public String processCheckout(
            @RequestParam("paymentMethod") String paymentMethod,
            HttpSession session,
            Principal principal,
            Model model) {
        System.out.println("Ã°Å¸Å¡â‚¬ Processing checkout");

        if (principal == null) {
            return "redirect:/login";
        }

        CheckoutForm checkoutForm = (CheckoutForm) session.getAttribute("pendingCheckoutForm");
        if (checkoutForm == null) {
            System.out.println("Ã¢ÂÅ’ No checkout form in session");
            return "redirect:/checkout";
        }

        try {
            User user = userService.getUserFromPrincipal(principal)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String fullShipping = checkoutForm.getFullShippingAddress();
            String billingAddr = checkoutForm.isSameBilling()
                    ? fullShipping
                    : (checkoutForm.getBillingAddress() != null ? checkoutForm.getBillingAddress() : fullShipping);

            System.out.println("Ã¢Å“â€¦ User found: " + user.getUsername());
            System.out.println("Ã°Å¸â€œÂ¦ Shipping address: " + fullShipping);
            System.out.println("Ã°Å¸â€™Â³ Payment method: " + paymentMethod);

            Order order = orderService.createOrderFromCart(
                    user.getId(), fullShipping, billingAddr, paymentMethod);

            model.addAttribute("user", user);
            model.addAttribute("order", order);
            model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));

            System.out.println("Ã°Å¸Å½â€° Order created successfully! Order #: " + order.getOrderNumber());

            session.removeAttribute("pendingCheckoutForm");
            return "order-confirmation";

        } catch (Exception e) {
            System.out.println("Ã¢ÂÅ’ ERROR processing checkout: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Checkout failed: " + e.getMessage());
            return "redirect:/cart";
        }
    }

    // ==================== API ENDPOINTS ====================

    @GetMapping("/api/products/featured")
    @ResponseBody
    public ApiResponse getFeaturedProducts() {
        System.out.println("Ã°Å¸â€œÂ¦ API: Get featured products");
        try {
            List<Product> products = productService.getFeaturedProducts();
            System.out.println("Ã¢Å“â€¦ API: Returning " + products.size() + " featured products");
            return new ApiResponse(true, "Success", products);
        } catch (Exception e) {
            System.out.println("Ã¢ÂÅ’ API ERROR getting featured products: " + e.getMessage());
            return new ApiResponse(false, "Failed to load products: " + e.getMessage(), null);
        }
    }

    @PostMapping("/api/cart/add")
    @ResponseBody
    public ApiResponse addToCart(@RequestBody CartRequest request, Principal principal) {
        System.out.println("Ã°Å¸â€ºâ€™ API: addToCart called");
        System.out.println("   Product ID: " + request.getProductId());
        System.out.println("   Quantity: " + request.getQuantity());

        if (principal == null) {
            System.out.println("Ã¢ÂÅ’ API: No authentication");
            return new ApiResponse(false, "Authentication required", null);
        }

        try {
            User user = userService.getUserFromPrincipal(principal).orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("Ã¢Å“â€¦ API: User found - ID: " + user.getId() + ", Name: " + user.getUsername());

            CartItem cartItem = cartService.addToCart(user.getId(), request.getProductId(), request.getQuantity());

            int cartCount = cartService.getCartItemCount(user.getId());

            System.out.println("Ã¢Å“â€¦ API: Success! Cart count: " + cartCount);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("cartCount", cartCount);
            responseData.put("success", true);
            responseData.put("message", "Item added to cart successfully");

            if (cartItem != null) {
                responseData.put("itemId", cartItem.getId());
                if (cartItem.getProduct() != null) {
                    responseData.put("productName", cartItem.getProduct().getName());
                    responseData.put("productPrice", cartItem.getProduct().getPrice());
                }
                responseData.put("quantity", cartItem.getQuantity());
                responseData.put("unitPrice", cartItem.getUnitPrice());
                responseData.put("totalPrice", cartItem.getTotalPrice());
            }

            return new ApiResponse(true, "Item added to cart successfully", responseData);

        } catch (Exception e) {
            System.out.println("Ã¢ÂÅ’ API ERROR in addToCart: " + e.getMessage());
            e.printStackTrace();
            return new ApiResponse(false, "Failed to add item: " + e.getMessage(), null);
        }
    }

    @GetMapping("/api/cart/items")
    @ResponseBody
    public ApiResponse getCartItems(Principal principal) {
        System.out.println("Ã°Å¸â€œâ€¹ API: Get cart items");

        if (principal == null) {
            return new ApiResponse(false, "Authentication required", null);
        }

        try {
            User user = userService.getUserFromPrincipal(principal)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Cart cart = cartService.getCartDetails(user.getId());

            Map<String, Object> cartData = new HashMap<>();
            cartData.put("id", cart.getId());
            cartData.put("totalItems", cart.getTotalItems());
            cartData.put("subtotal", cart.getSubtotal());
            cartData.put("shipping", cart.getShipping());
            cartData.put("total", cart.getTotal());
            cartData.put("isEmpty", cart.isEmpty());

            List<Map<String, Object>> itemsData = new ArrayList<>();
            for (CartItem item : cart.getItems()) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("id", item.getId());
                itemData.put("quantity", item.getQuantity());
                itemData.put("unitPrice", item.getUnitPrice());
                itemData.put("totalPrice", item.getTotalPrice());

                if (item.getProduct() != null) {
                    Map<String, Object> productData = new HashMap<>();
                    productData.put("id", item.getProduct().getId());
                    productData.put("name", item.getProduct().getName());
                    productData.put("price", item.getProduct().getPrice());
                    productData.put("category", item.getProduct().getCategory());
                    productData.put("imageUrl", item.getProduct().getImageUrl());
                    itemData.put("product", productData);
                }

                itemsData.add(itemData);
            }
            cartData.put("items", itemsData);

            System.out.println("Ã¢Å“â€¦ API: Returning cart with " + itemsData.size() + " items");
            return new ApiResponse(true, "Success", cartData);

        } catch (Exception e) {
            System.out.println("Ã¢ÂÅ’ API ERROR getting cart items: " + e.getMessage());
            return new ApiResponse(false, "Failed to load cart: " + e.getMessage(), null);
        }
    }

    @PostMapping("/api/cart/update")
    @ResponseBody
    public ApiResponse updateCartItem(@RequestBody UpdateCartRequest request, Principal principal) {
        System.out.println("Ã°Å¸â€œÂ API: Update cart item");
        System.out.println("   Item ID: " + request.getItemId());
        System.out.println("   Quantity: " + request.getQuantity());

        if (principal == null) {
            return new ApiResponse(false, "Authentication required", null);
        }

        try {
            User user = userService.getUserFromPrincipal(principal)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            CartItem updatedItem = cartService.updateCartItem(user.getId(), request.getItemId(), request.getQuantity());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            if (updatedItem != null) {
                responseData.put("itemId", updatedItem.getId());
                responseData.put("quantity", updatedItem.getQuantity());
                if (updatedItem.getProduct() != null) {
                    responseData.put("productName", updatedItem.getProduct().getName());
                }
            }
            responseData.put("cartCount", cartService.getCartItemCount(user.getId()));

            System.out.println("Ã¢Å“â€¦ API: Cart item updated");
            return new ApiResponse(true, "Cart updated successfully", responseData);

        } catch (Exception e) {
            System.out.println("Ã¢ÂÅ’ API ERROR updating cart: " + e.getMessage());
            return new ApiResponse(false, "Failed to update cart: " + e.getMessage(), null);
        }
    }

    @PostMapping("/api/cart/remove")
    @ResponseBody
    public ApiResponse removeCartItem(@RequestBody RemoveCartRequest request, Principal principal) {
        System.out.println("Ã°Å¸â€”â€˜Ã¯Â¸Â API: Remove cart item");
        System.out.println("   Item ID: " + request.getItemId());

        if (principal == null) {
            return new ApiResponse(false, "Authentication required", null);
        }

        try {
            User user = userService.getUserFromPrincipal(principal)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            cartService.removeFromCart(user.getId(), request.getItemId());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Item removed from cart");
            responseData.put("cartCount", cartService.getCartItemCount(user.getId()));

            System.out.println("Ã¢Å“â€¦ API: Cart item removed");
            return new ApiResponse(true, "Item removed from cart", responseData);

        } catch (Exception e) {
            System.out.println("Ã¢ÂÅ’ API ERROR removing cart item: " + e.getMessage());
            return new ApiResponse(false, "Failed to remove item: " + e.getMessage(), null);
        }
    }

    @GetMapping("/api/cart/count")
    @ResponseBody
    public ApiResponse getCartCount(Principal principal) {
        System.out.println("Ã°Å¸â€Â¢ API: Get cart count");

        if (principal == null) {
            return new ApiResponse(false, "Authentication required", null);
        }

        try {
            User user = userService.getUserFromPrincipal(principal)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            int cartCount = cartService.getCartItemCount(user.getId());

            System.out.println("Ã¢Å“â€¦ API: Cart count = " + cartCount);
            return new ApiResponse(true, "Success", Map.of("cartCount", cartCount));

        } catch (Exception e) {
            System.out.println("Ã¢ÂÅ’ API ERROR getting cart count: " + e.getMessage());
            return new ApiResponse(false, "Failed to get cart count: " + e.getMessage(), null);
        }
    }

    @GetMapping("/orders/history")
    @ResponseBody
    public List<Order> getOrderHistory(Principal principal) {
        System.out.println("Ã°Å¸â€œÅ“ API: Get order history");

        if (principal == null) {
            return Collections.emptyList();
        }

        try {
            User user = userService.getUserFromPrincipal(principal)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Order> orders = orderService.getUserOrders(user.getId());
            System.out.println("Ã¢Å“â€¦ API: Returning " + orders.size() + " orders");
            return orders;

        } catch (Exception e) {
            System.out.println("Ã¢ÂÅ’ API ERROR getting order history: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== DEBUG ENDPOINTS ====================

    @GetMapping("/debug/cart")
    public String debugCart(Model model, Principal principal) {
        System.out.println("Ã°Å¸Ââ€º DEBUG: Cart test page");

        if (principal != null) {
            try {
                User user = userService.findByEmail(principal.getName())
                        .orElseGet(() -> userService.findByUsername(principal.getName()).orElse(null));
                if (user != null) {
                    model.addAttribute("user", user);
                    model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
                }
            } catch (Exception e) {
                System.out.println("Ã¢Å¡Â Ã¯Â¸Â DEBUG: Could not load user info: " + e.getMessage());
            }
        }

        return "debug-cart";
    }

    @GetMapping("/api/debug/users")
    @ResponseBody
    public ApiResponse debugGetUsers() {
        System.out.println("Ã°Å¸Ââ€º DEBUG: Get all users");
        try {
            List<User> users = userService.getAllUsers();
            List<Map<String, Object>> userList = new ArrayList<>();
            for (User user : users) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("username", user.getUsername());
                userData.put("email", user.getEmail());
                userData.put("cartId", user.getCart() != null ? user.getCart().getId() : "No cart");
                userList.add(userData);
            }
            return new ApiResponse(true, "Success", userList);
        } catch (Exception e) {
            return new ApiResponse(false, "Error: " + e.getMessage(), null);
        }
    }

    @GetMapping("/api/debug/products")
    @ResponseBody
    public ApiResponse debugGetProducts() {
        System.out.println("Ã°Å¸Ââ€º DEBUG: Get all products");
        try {
            List<Product> products = productService.findAllProducts();
            return new ApiResponse(true, "Success", products);
        } catch (Exception e) {
            return new ApiResponse(false, "Error: " + e.getMessage(), null);
        }
    }

    // ==================== HELPER METHODS ====================

    private double calculatePlasticSaved(Integer orderCount) {
        if (orderCount == null)
            return 0.0;
        return orderCount * 0.5;
    }

    private double calculateWaterSaved(Integer orderCount) {
        if (orderCount == null)
            return 0.0;
        return orderCount * 10.0;
    }

    private double calculateCarbonSaved(Integer orderCount) {
        if (orderCount == null)
            return 0.0;
        return orderCount * 2.5;
    }

    // ==================== SUPPORT CLASSES ====================

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

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

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }

    public static class SimpleUserDTO {
        private String username;
        private String initial;

        public SimpleUserDTO(String username, String initial) {
            this.username = username;
            this.initial = initial;
        }

        public String getUsername() {
            return username;
        }

        public String getInitial() {
            return initial;
        }
    }

    public static class CartRequest {
        private Long productId;
        private Integer quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    public static class UpdateCartRequest {
        private Long itemId;
        private Integer quantity;

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    public static class RemoveCartRequest {
        private Long itemId;

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }
    }
}





