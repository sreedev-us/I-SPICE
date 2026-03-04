package com.company.I_SPICE.services;

import com.company.I_SPICE.model.Product;
import com.company.I_SPICE.model.Review;
import com.company.I_SPICE.model.User;
import com.company.I_SPICE.repository.ProductRepository;
import com.company.I_SPICE.repository.ReviewRepository;
import com.company.I_SPICE.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Review submitReview(Long productId, String username, int rating, String comment) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new RuntimeException("User not found")));

        // Create and save review
        Review review = new Review(product, user, rating, comment);
        Review savedReview = reviewRepository.save(review);

        // Update product average rating
        Double avgRating = reviewRepository.findAverageRatingByProductId(productId);
        product.setAverageRating(avgRating != null ? avgRating : (double) rating);
        productRepository.save(product);

        return savedReview;
    }

    public List<Review> getProductReviews(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public long getReviewCount(Long productId) {
        return reviewRepository.countByProductId(productId);
    }
}
