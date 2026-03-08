package com.company.I_SPICE.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String category;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "additional_images", columnDefinition = "TEXT")
    private String additionalImages; // comma-separated list of extra image URLs

    /**
     * Returns the additional images as a list, stripping empty entries.
     */
    public List<String> getAdditionalImageList() {
        if (additionalImages == null || additionalImages.isBlank())
            return Collections.emptyList();
        return Arrays.stream(additionalImages.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean featured = false;

    private int stock;

    // New fields for spices
    @Column(name = "weight_grams")
    private Integer weightGrams;

    private String origin;

    private Boolean organic = false;

    @Column(name = "spicy_level")
    private Integer spicyLevel; // 1-5 scale

    @Column(name = "sales_count")
    private Long salesCount = 0L;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    private Integer discount = 0;

    @Column(name = "discounted_price")
    private Double discountedPrice;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (salesCount == null)
            salesCount = 0L;
        if (averageRating == null)
            averageRating = 0.0;
        if (discount == null)
            discount = 0;
        calculateDiscountedPrice();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateDiscountedPrice();
    }

    @PostLoad
    protected void onPostLoad() {
        if (discountedPrice == null) {
            calculateDiscountedPrice();
        }
    }

    private void calculateDiscountedPrice() {
        if (price == null) {
            this.discountedPrice = 0.0;
            return;
        }
        if (discount == null || discount <= 0) {
            this.discountedPrice = price.doubleValue();
        } else {
            double originalPrice = price.doubleValue();
            this.discountedPrice = originalPrice * (1 - (discount / 100.0));
        }
    }

    // Getters and setters for all fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAdditionalImages() {
        return additionalImages;
    }

    public void setAdditionalImages(String additionalImages) {
        this.additionalImages = additionalImages;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public Integer getWeightGrams() {
        return weightGrams;
    }

    public void setWeightGrams(Integer weightGrams) {
        this.weightGrams = weightGrams;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Boolean getOrganic() {
        return organic;
    }

    public void setOrganic(Boolean organic) {
        this.organic = organic;
    }

    public Integer getSpicyLevel() {
        return spicyLevel;
    }

    public void setSpicyLevel(Integer spicyLevel) {
        this.spicyLevel = spicyLevel;
    }

    public Long getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(Long salesCount) {
        this.salesCount = salesCount;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getDiscount() {
        return discount;
    }

    public void setDiscount(Integer discount) {
        this.discount = discount;
    }

    public Double getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(Double discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}