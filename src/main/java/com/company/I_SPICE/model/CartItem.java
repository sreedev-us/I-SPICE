package com.company.I_SPICE.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity = 1;

    // Map BOTH columns since they both exist in database
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public CartItem() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // Initialize both fields to avoid null
        this.price = BigDecimal.ZERO;
        this.unitPrice = BigDecimal.ZERO;
    }

    public CartItem(Cart cart, Product product, Integer quantity) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
        BigDecimal productPrice = product != null ? product.getDiscountedPrice() : BigDecimal.ZERO;
        this.price = productPrice;
        this.unitPrice = productPrice; // Set both to same value
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
        this.updatedAt = LocalDateTime.now();
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            // Set both price fields when product is set
            if (this.price == null || this.price.equals(BigDecimal.ZERO)) {
                this.price = product.getDiscountedPrice();
            }
            if (this.unitPrice == null || this.unitPrice.equals(BigDecimal.ZERO)) {
                this.unitPrice = product.getDiscountedPrice();
            }
        }
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity != null ? quantity : 1;
        this.updatedAt = LocalDateTime.now();
    }

    // Primary getter/setter for price field
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        this.unitPrice = price; // Keep them synchronized
        this.updatedAt = LocalDateTime.now();
    }

    // Getter/setter for unitPrice field
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        this.price = unitPrice; // Keep them synchronized
        this.updatedAt = LocalDateTime.now();
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

    // Calculate total price for this item
    public BigDecimal getTotalPrice() {
        // Use price field for calculations
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    @PrePersist
    protected void onCreate() {
        System.out.println("🔄 CartItem @PrePersist called");
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }

        // Debug output
        System.out.println("   Price field value: " + price);
        System.out.println("   UnitPrice field value: " + unitPrice);
        System.out.println("   Product: " + (product != null ? product.getName() : "null"));
        System.out.println("   Product price: " + (product != null ? product.getPrice() : "null"));

        // Ensure both fields are set and synchronized
        if (price == null && unitPrice != null) {
            price = unitPrice;
            System.out.println("   Auto-set price from unitPrice: " + price);
        } else if (unitPrice == null && price != null) {
            unitPrice = price;
            System.out.println("   Auto-set unitPrice from price: " + unitPrice);
        } else if (price == null && product != null) {
            // Both are null, set from product
            price = product.getDiscountedPrice();
            unitPrice = product.getDiscountedPrice();
            System.out.println("   Auto-set both from product: " + price);
        } else if (price != null && unitPrice != null && !price.equals(unitPrice)) {
            // They're different, sync them (use price as source of truth)
            unitPrice = price;
            System.out.println("   Synced unitPrice to match price: " + unitPrice);
        }

        // Final check - ensure neither is null
        if (price == null) {
            price = BigDecimal.ZERO;
            System.out.println("⚠️  Price was null, set to ZERO");
        }
        if (unitPrice == null) {
            unitPrice = BigDecimal.ZERO;
            System.out.println("⚠️  UnitPrice was null, set to ZERO");
        }

        System.out.println("   Final - Price: " + price + ", UnitPrice: " + unitPrice);
    }

    @PreUpdate
    protected void onUpdate() {
        System.out.println("🔄 CartItem @PreUpdate called");
        updatedAt = LocalDateTime.now();

        // Ensure synchronization on update too
        if (price != null && unitPrice == null) {
            unitPrice = price;
        } else if (unitPrice != null && price == null) {
            price = unitPrice;
        } else if (price != null && unitPrice != null && !price.equals(unitPrice)) {
            // If they differ, use price as source of truth
            unitPrice = price;
        }
    }
}