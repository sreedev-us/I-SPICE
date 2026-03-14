package com.company.I_SPICE.services;

import com.company.I_SPICE.model.Product;
import com.company.I_SPICE.model.User;
import com.company.I_SPICE.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final SubscriptionPlanService subscriptionPlanService;

    public ProductService(ProductRepository productRepository, SubscriptionPlanService subscriptionPlanService) {
        this.productRepository = productRepository;
        this.subscriptionPlanService = subscriptionPlanService;
    }

    @Cacheable("products")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Add this alias method for compatibility
    @Cacheable("products")
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    @Cacheable("products_featured")
    public List<Product> getFeaturedProducts() {
        return productRepository.findByFeaturedTrue();
    }

    public List<Product> getFeaturedProducts(User user) {
        return filterAccessibleProducts(productRepository.findByFeaturedTrue(), user);
    }

    @Cacheable(value = "products_category", key = "#category")
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Cacheable(value = "product", key = "#id")
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Optional<Product> getAccessibleProductById(Long id, User user) {
        return productRepository.findById(id)
                .filter(product -> canUserAccessProduct(user, product));
    }

    @Transactional
    @CacheEvict(value = { "products", "products_featured", "products_category", "product" }, allEntries = true)
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = { "products", "products_featured", "products_category", "product" }, allEntries = true)
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    // Advanced Search with Filters
    public Page<Product> searchProducts(String search, String category, Double minPrice, Double maxPrice,
            Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by name or description
            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate namePredicate = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate descPredicate = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(namePredicate, descPredicate));
            }

            // Filter by category
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            // Filter by price range
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable);
    }

    // Autocomplete
    public List<String> searchProductNames(String term) {
        if (!StringUtils.hasText(term)) {
            return List.of();
        }
        return productRepository.findByNameContainingIgnoreCase(term)
                .stream()
                .map(Product::getName)
                .limit(5)
                .collect(Collectors.toList());
    }

    // Filter Limits
    public Double getMinPrice() {
        Double min = productRepository.findMinPrice();
        return min != null ? min : 0.0;
    }

    public Double getMaxPrice() {
        Double max = productRepository.findMaxPrice();
        return max != null ? max : 1000.0;
    }

    // Category Counts
    public Map<String, Long> getCategoryCounts() {
        List<Object[]> results = productRepository.countProductsByCategory();
        Map<String, Long> counts = new HashMap<>();
        for (Object[] result : results) {
            String category = (String) result[0];
            Long count = (Long) result[1];
            counts.put(category, count);
        }
        return counts;
    }

    // Related Products
    public List<Product> getRelatedProducts(String category, Long excludeId) {
        return productRepository.findByCategory(category)
                .stream()
                .filter(p -> !p.getId().equals(excludeId))
                .limit(4)
                .collect(Collectors.toList());
    }

    public List<Product> getRelatedProducts(String category, Long excludeId, User user) {
        return filterAccessibleProducts(getRelatedProducts(category, excludeId), user);
    }

    public List<Product> filterAccessibleProducts(List<Product> products, User user) {
        return products.stream()
                .filter(product -> canUserAccessProduct(user, product))
                .collect(Collectors.toList());
    }

    public boolean canUserAccessProduct(User user, Product product) {
        if (product == null) {
            return false;
        }
        String requiredPlan = product.getSubscriptionTierRequired();
        boolean hasRequiredPlan = subscriptionPlanService.userHasRequiredPlan(user, requiredPlan);
        if (!hasRequiredPlan) {
            return requiredPlan == null || requiredPlan.isBlank();
        }
        if (Boolean.TRUE.equals(product.getEarlyAccessOnly())) {
            return user != null && user.hasActiveSubscription();
        }
        return true;
    }

    // Wishlist (Stub)
    public boolean isProductInWishlist(User user, Product product) {
        if (user == null || product == null)
            return false;
        // TODO: Implement actual wishlist logic
        return false;
    }

    public List<Product> getWishlist(Long userId) {
        // TODO: Implement wishlist functionality
        return List.of();
    }

    public List<Product> getUserSubscriptions(Long userId) {
        // TODO: Implement subscription functionality
        return List.of();
    }
}
