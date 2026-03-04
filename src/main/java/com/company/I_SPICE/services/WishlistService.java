package com.company.I_SPICE.services;

import com.company.I_SPICE.model.Product;
import com.company.I_SPICE.model.User;
import com.company.I_SPICE.model.Wishlist;
import com.company.I_SPICE.repository.WishlistRepository;
import com.company.I_SPICE.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    public WishlistService(WishlistRepository wishlistRepository, ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
    }

    /**
     * Get all products in a user's wishlist (most recently added first)
     */
    public List<Product> getWishlistProducts(Long userId) {
        List<Product> products = wishlistRepository.findProductsByUserId(userId);
        // Populate discounted price for display
        products.forEach(p -> {
            if (p.getDiscount() != null && p.getDiscount() > 0) {
                double dp = p.getPrice().doubleValue() * (1 - p.getDiscount() / 100.0);
                p.setDiscountedPrice(Math.round(dp * 100.0) / 100.0);
            } else {
                p.setDiscountedPrice(p.getPrice().doubleValue());
            }
        });
        return products;
    }

    /**
     * Add a product to user's wishlist. Returns false if already present.
     */
    @Transactional
    public boolean addToWishlist(User user, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            return false; // already in wishlist
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        wishlistRepository.save(new Wishlist(user, product));
        return true;
    }

    /**
     * Remove a product from user's wishlist.
     */
    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    /**
     * Check if a product is in the user's wishlist.
     */
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    /**
     * Count wishlist items for a user.
     */
    public long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }
}
