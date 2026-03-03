package com.company.I_SPICE.services;

import com.company.I_SPICE.model.*;
import com.company.I_SPICE.repository.CartItemRepository;
import com.company.I_SPICE.repository.CartRepository;
import com.company.I_SPICE.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       UserService userService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userService = userService;
    }

    // ==================== CART MANAGEMENT ====================

    /**
     * Get or create cart for a user
     */
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        System.out.println("🔍 CartService.getOrCreateCart() called");
        System.out.println("   User ID: " + userId);

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        try {
            // First, try to find existing cart
            Optional<Cart> cartOptional = cartRepository.findByUserId(userId);
            if (cartOptional.isPresent()) {
                Cart cart = cartOptional.get();
                System.out.println("✅ Found existing cart - ID: " + cart.getId());
                System.out.println("   Cart has " + cart.getItems().size() + " items");
                return cart;
            }

            System.out.println("🔄 No cart found, creating new one...");

            // Find the user
            User user = userService.findById(userId)
                    .orElseThrow(() -> {
                        System.out.println("❌ User not found with ID: " + userId);
                        return new RuntimeException("User not found with id: " + userId);
                    });

            System.out.println("✅ User found: " + user.getUsername() + " (ID: " + user.getId() + ")");

            // Check if user already has a cart through the relationship
            if (user.getCart() != null) {
                System.out.println("📦 User already has cart via relationship - ID: " + user.getCart().getId());
                return user.getCart();
            }

            // Create new cart
            Cart newCart = new Cart();
            newCart.setUser(user);
            newCart.setCreatedAt(LocalDateTime.now());
            newCart.setUpdatedAt(LocalDateTime.now());

            System.out.println("💾 Saving new cart...");
            Cart savedCart = cartRepository.save(newCart);
            System.out.println("✅ Cart saved successfully - ID: " + savedCart.getId());

            // Update user's cart reference
            user.setCart(savedCart);

            return savedCart;

        } catch (Exception e) {
            System.out.println("❌ ERROR in getOrCreateCart: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get or create cart: " + e.getMessage(), e);
        }
    }

    /**
     * Add item to cart
     */
    @Transactional
    public CartItem addToCart(Long userId, Long productId, Integer quantity) {
        System.out.println("🛒 CartService.addToCart() called");
        System.out.println("   User ID: " + userId);
        System.out.println("   Product ID: " + productId);
        System.out.println("   Quantity: " + quantity);

        if (quantity == null || quantity < 1) {
            quantity = 1;
            System.out.println("   Quantity set to default: 1");
        }

        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }

        try {
            // Step 1: Get or create cart
            System.out.println("📋 Step 1: Getting or creating cart...");
            Cart cart = getOrCreateCart(userId);
            System.out.println("✅ Using cart ID: " + cart.getId());

            // Step 2: Find the product
            System.out.println("📋 Step 2: Finding product with ID: " + productId);
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> {
                        System.out.println("❌ Product not found with ID: " + productId);
                        return new RuntimeException("Product not found with id: " + productId);
                    });

            System.out.println("✅ Product found: " + product.getName());
            System.out.println("   Price: " + product.getPrice());
            System.out.println("   Stock: " + product.getStock());

            // Check stock availability
            if (product.getStock() < quantity) {
                System.out.println("❌ Insufficient stock. Available: " + product.getStock() + ", Requested: " + quantity);
                throw new RuntimeException("Insufficient stock. Only " + product.getStock() + " items available.");
            }

            // Step 3: Check if product already exists in cart
            System.out.println("📋 Step 3: Checking if product already in cart...");
            Optional<CartItem> existingItemOptional = cartItemRepository.findByCartAndProduct(cart, product);

            if (existingItemOptional.isPresent()) {
                System.out.println("📝 Product already in cart, updating quantity");
                CartItem existingItem = existingItemOptional.get();

                // Check total quantity won't exceed stock
                int newTotalQuantity = existingItem.getQuantity() + quantity;
                if (product.getStock() < newTotalQuantity) {
                    System.out.println("❌ Insufficient stock for additional quantity");
                    throw new RuntimeException("Cannot add more items. Total would exceed available stock.");
                }

                existingItem.setQuantity(newTotalQuantity);
                existingItem.setUpdatedAt(LocalDateTime.now());

                System.out.println("💾 Saving updated cart item...");
                CartItem savedItem = cartItemRepository.save(existingItem);
                System.out.println("✅ Cart item updated - ID: " + savedItem.getId());
                System.out.println("   New quantity: " + savedItem.getQuantity());

                // Update cart timestamp
                cart.setUpdatedAt(LocalDateTime.now());
                cartRepository.save(cart);

                return savedItem;

            } else {
                System.out.println("➕ Product not in cart, creating new cart item");

                // Create new cart item - FIXED: Use setPrice() instead of setUnitPrice()
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setProduct(product);
                newItem.setQuantity(quantity);

                // FIX: This is the key change - use setPrice() instead of setUnitPrice()
                newItem.setPrice(product.getPrice());

                newItem.setCreatedAt(LocalDateTime.now());
                newItem.setUpdatedAt(LocalDateTime.now());

                System.out.println("💾 Saving new cart item...");
                System.out.println("   Item price set to: " + newItem.getPrice());
                CartItem savedItem = cartItemRepository.save(newItem);
                System.out.println("✅ New cart item saved - ID: " + savedItem.getId());
                System.out.println("   Saved price: " + savedItem.getPrice());

                // Add to cart's items list
                cart.getItems().add(savedItem);
                cart.setUpdatedAt(LocalDateTime.now());
                cartRepository.save(cart);

                System.out.println("🎉 Item added to cart successfully!");
                return savedItem;
            }

        } catch (RuntimeException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            System.out.println("❌ ERROR in addToCart: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to add item to cart: " + e.getMessage(), e);
        }
    }

    /**
     * Update cart item quantity
     */
    @Transactional
    public CartItem updateCartItem(Long userId, Long itemId, Integer quantity) {
        System.out.println("📝 CartService.updateCartItem() called");
        System.out.println("   User ID: " + userId);
        System.out.println("   Item ID: " + itemId);
        System.out.println("   New Quantity: " + quantity);

        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Quantity must be zero or positive");
        }

        if (itemId == null) {
            throw new IllegalArgumentException("Item ID cannot be null");
        }

        try {
            // Get user's cart
            Cart cart = getOrCreateCart(userId);

            // Find the cart item
            CartItem cartItem = cartItemRepository.findById(itemId)
                    .orElseThrow(() -> {
                        System.out.println("❌ Cart item not found with ID: " + itemId);
                        return new RuntimeException("Cart item not found with id: " + itemId);
                    });

            // Verify the item belongs to user's cart
            if (!cartItem.getCart().getId().equals(cart.getId())) {
                System.out.println("❌ Cart item does not belong to user's cart");
                throw new RuntimeException("Cart item does not belong to user's cart");
            }

            Product product = cartItem.getProduct();

            if (quantity == 0) {
                // Remove item from cart
                System.out.println("🗑️ Removing item from cart (quantity = 0)");
                cart.removeItem(cartItem);
                cartItemRepository.delete(cartItem);
                cart.setUpdatedAt(LocalDateTime.now());
                cartRepository.save(cart);
                System.out.println("✅ Item removed from cart");
                return null;

            } else {
                // Check stock availability
                if (product.getStock() < quantity) {
                    System.out.println("❌ Insufficient stock. Available: " + product.getStock() + ", Requested: " + quantity);
                    throw new RuntimeException("Insufficient stock. Only " + product.getStock() + " items available.");
                }

                // Update quantity
                System.out.println("✏️ Updating item quantity from " + cartItem.getQuantity() + " to " + quantity);
                cartItem.setQuantity(quantity);
                cartItem.setUpdatedAt(LocalDateTime.now());

                CartItem savedItem = cartItemRepository.save(cartItem);

                // Update cart timestamp
                cart.setUpdatedAt(LocalDateTime.now());
                cartRepository.save(cart);

                System.out.println("✅ Cart item updated successfully");
                return savedItem;
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("❌ ERROR in updateCartItem: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update cart item: " + e.getMessage(), e);
        }
    }

    /**
     * Remove item from cart
     */
    @Transactional
    public void removeFromCart(Long userId, Long itemId) {
        System.out.println("🗑️ CartService.removeFromCart() called");
        System.out.println("   User ID: " + userId);
        System.out.println("   Item ID: " + itemId);

        if (itemId == null) {
            throw new IllegalArgumentException("Item ID cannot be null");
        }

        try {
            // Get user's cart
            Cart cart = getOrCreateCart(userId);

            // Find the cart item
            CartItem cartItem = cartItemRepository.findById(itemId)
                    .orElseThrow(() -> {
                        System.out.println("❌ Cart item not found with ID: " + itemId);
                        return new RuntimeException("Cart item not found with id: " + itemId);
                    });

            // Verify the item belongs to user's cart
            if (!cartItem.getCart().getId().equals(cart.getId())) {
                System.out.println("❌ Cart item does not belong to user's cart");
                throw new RuntimeException("Cart item does not belong to user's cart");
            }

            // Remove item from cart
            cart.removeItem(cartItem);
            cartItemRepository.delete(cartItem);

            // Update cart timestamp
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);

            System.out.println("✅ Item removed from cart successfully");

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("❌ ERROR in removeFromCart: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to remove item from cart: " + e.getMessage(), e);
        }
    }

    /**
     * Clear all items from cart
     */
    @Transactional
    public void clearCart(Long userId) {
        System.out.println("🧹 CartService.clearCart() called");
        System.out.println("   User ID: " + userId);

        try {
            Cart cart = getOrCreateCart(userId);

            System.out.println("📦 Cart has " + cart.getItems().size() + " items to clear");

            // Clear all items
            for (CartItem item : cart.getItems()) {
                cartItemRepository.delete(item);
            }

            cart.getItems().clear();
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);

            System.out.println("✅ Cart cleared successfully");

        } catch (Exception e) {
            System.out.println("❌ ERROR in clearCart: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to clear cart: " + e.getMessage(), e);
        }
    }

    // ==================== GETTER METHODS ====================

    /**
     * Get total number of items in cart
     */
    public int getCartItemCount(Long userId) {
        try {
            System.out.println("🔢 CartService.getCartItemCount() called");
            System.out.println("   User ID: " + userId);

            Cart cart = getOrCreateCart(userId);
            int count = cart.getTotalItems();

            System.out.println("✅ Cart item count: " + count);
            return count;

        } catch (Exception e) {
            System.out.println("⚠️ Could not get cart count for user " + userId + ": " + e.getMessage());
            return 0; // Return 0 if cart cannot be accessed
        }
    }

    /**
     * Get cart details with all items
     */
    public Cart getCartDetails(Long userId) {
        System.out.println("📋 CartService.getCartDetails() called");
        System.out.println("   User ID: " + userId);

        try {
            Cart cart = getOrCreateCart(userId);
            System.out.println("✅ Returning cart with " + cart.getItems().size() + " items");
            return cart;
        } catch (Exception e) {
            System.out.println("❌ ERROR in getCartDetails: " + e.getMessage());
            throw new RuntimeException("Failed to get cart details: " + e.getMessage(), e);
        }
    }

    /**
     * Get cart subtotal
     */
    public double getCartSubtotal(Long userId) {
        try {
            Cart cart = getOrCreateCart(userId);
            return cart.getSubtotal().doubleValue();
        } catch (Exception e) {
            System.out.println("⚠️ Could not get cart subtotal: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Get cart total including shipping and tax
     */
    public double getCartTotal(Long userId) {
        try {
            Cart cart = getOrCreateCart(userId);
            return cart.getTotal().doubleValue();
        } catch (Exception e) {
            System.out.println("⚠️ Could not get cart total: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Check if cart is empty
     */
    public boolean isCartEmpty(Long userId) {
        try {
            Cart cart = getOrCreateCart(userId);
            return cart.isEmpty();
        } catch (Exception e) {
            System.out.println("⚠️ Could not check if cart is empty: " + e.getMessage());
            return true;
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if product is already in cart
     */
    public boolean isProductInCart(Long userId, Long productId) {
        try {
            Cart cart = getOrCreateCart(userId);
            return cart.getItems().stream()
                    .anyMatch(item -> item.getProduct() != null &&
                            item.getProduct().getId().equals(productId));
        } catch (Exception e) {
            System.out.println("⚠️ Could not check if product is in cart: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get quantity of specific product in cart
     */
    public int getProductQuantityInCart(Long userId, Long productId) {
        try {
            Cart cart = getOrCreateCart(userId);
            return cart.getItems().stream()
                    .filter(item -> item.getProduct() != null &&
                            item.getProduct().getId().equals(productId))
                    .findFirst()
                    .map(CartItem::getQuantity)
                    .orElse(0);
        } catch (Exception e) {
            System.out.println("⚠️ Could not get product quantity in cart: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Merge two carts (useful when user logs in)
     */
    @Transactional
    public void mergeCarts(Long targetUserId, Long sourceCartId) {
        System.out.println("🔄 CartService.mergeCarts() called");
        System.out.println("   Target User ID: " + targetUserId);
        System.out.println("   Source Cart ID: " + sourceCartId);

        try {
            Cart targetCart = getOrCreateCart(targetUserId);
            Cart sourceCart = cartRepository.findById(sourceCartId)
                    .orElseThrow(() -> new RuntimeException("Source cart not found"));

            System.out.println("📦 Merging " + sourceCart.getItems().size() + " items into target cart");

            for (CartItem sourceItem : sourceCart.getItems()) {
                Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(
                        targetCart, sourceItem.getProduct());

                if (existingItem.isPresent()) {
                    // Update quantity
                    CartItem targetItem = existingItem.get();
                    targetItem.setQuantity(targetItem.getQuantity() + sourceItem.getQuantity());
                    cartItemRepository.save(targetItem);
                } else {
                    // Create new item
                    CartItem newItem = new CartItem();
                    newItem.setCart(targetCart);
                    newItem.setProduct(sourceItem.getProduct());
                    newItem.setQuantity(sourceItem.getQuantity());

                    // FIX: Use setPrice() instead of setUnitPrice()
                    newItem.setPrice(sourceItem.getPrice());

                    newItem.setCreatedAt(LocalDateTime.now());
                    newItem.setUpdatedAt(LocalDateTime.now());
                    cartItemRepository.save(newItem);
                    targetCart.getItems().add(newItem);
                }
            }

            // Update target cart timestamp
            targetCart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(targetCart);

            // Delete source cart
            cartRepository.delete(sourceCart);

            System.out.println("✅ Carts merged successfully");

        } catch (Exception e) {
            System.out.println("❌ ERROR merging carts: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to merge carts: " + e.getMessage(), e);
        }
    }

    // ==================== DEBUG METHODS ====================

    /**
     * Debug method to print cart details
     */
    public void printCartDetails(Long userId) {
        try {
            Cart cart = getOrCreateCart(userId);
            System.out.println("=== CART DEBUG INFO ===");
            System.out.println("Cart ID: " + cart.getId());
            System.out.println("User ID: " + cart.getUser().getId());
            System.out.println("Total Items: " + cart.getTotalItems());
            System.out.println("Subtotal: ₹" + cart.getSubtotal());
            System.out.println("Shipping: ₹" + cart.getShipping());
            System.out.println("Total: ₹" + cart.getTotal());
            System.out.println("Items:");

            if (cart.getItems().isEmpty()) {
                System.out.println("  (empty)");
            } else {
                for (CartItem item : cart.getItems()) {
                    System.out.println("  - " + item.getProduct().getName() +
                            " x" + item.getQuantity() +
                            " @ ₹" + item.getPrice() + // FIX: Use getPrice() instead of getUnitPrice()
                            " = ₹" + item.getTotalPrice());
                }
            }
            System.out.println("======================");
        } catch (Exception e) {
            System.out.println("❌ Could not print cart details: " + e.getMessage());
        }
    }

    /**
     * Debug: Test the CartItem entity setup
     */
    @Transactional
    public void testCartItemCreation(Long userId, Long productId) {
        System.out.println("🧪 DEBUG: Testing CartItem creation");
        System.out.println("   User ID: " + userId);
        System.out.println("   Product ID: " + productId);

        try {
            Cart cart = getOrCreateCart(userId);
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // Test creating a CartItem
            CartItem testItem = new CartItem();
            testItem.setCart(cart);
            testItem.setProduct(product);
            testItem.setQuantity(1);
            testItem.setPrice(product.getPrice()); // This is the key line

            System.out.println("   CartItem price field value: " + testItem.getPrice());
            System.out.println("   Using getPrice(): " + testItem.getPrice());
            System.out.println("   Using getUnitPrice(): " + testItem.getUnitPrice());

            // Try to save it
            CartItem saved = cartItemRepository.save(testItem);
            System.out.println("✅ DEBUG: CartItem saved successfully with ID: " + saved.getId());
            System.out.println("   Saved price: " + saved.getPrice());

            // Clean up
            cartItemRepository.delete(saved);
            System.out.println("✅ DEBUG: Test completed successfully");

        } catch (Exception e) {
            System.out.println("❌ DEBUG ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}