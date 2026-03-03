package com.company.I_SPICE.repository;

import com.company.I_SPICE.model.Wishlist;
import com.company.I_SPICE.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // Get all wishlist entries for a user
    List<Wishlist> findByUserIdOrderByAddedAtDesc(Long userId);

    // Check if a product is already wishlisted by a user
    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

    // Delete by user + product
    void deleteByUserIdAndProductId(Long userId, Long productId);

    // Count wishlist items for a user
    long countByUserId(Long userId);

    // Check existence
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // Get only the products from a user's wishlist
    @Query("SELECT w.product FROM Wishlist w WHERE w.user.id = :userId ORDER BY w.addedAt DESC")
    List<Product> findProductsByUserId(Long userId);
}
