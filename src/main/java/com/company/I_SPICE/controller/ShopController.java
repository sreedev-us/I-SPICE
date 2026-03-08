package com.company.I_SPICE.controller;

import com.company.I_SPICE.model.Product;
import com.company.I_SPICE.model.User;
import com.company.I_SPICE.model.Review;
import com.company.I_SPICE.services.ProductService;
import com.company.I_SPICE.services.CartService;
import com.company.I_SPICE.services.UserService;
import com.company.I_SPICE.services.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
public class ShopController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewService reviewService;

    // List of available categories
    private static final List<String> CATEGORIES = Arrays.asList(
            "Whole Spices", "Ground Spices", "Spice Blends", "Herbs & Seeds",
            "Organic Spices", "Exotic Spices", "Salts & Seasonings", "Gift Sets", "Premium Spices");

    @GetMapping("/shop")
    public String shopPage(Model model,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Principal principal) {

        System.out.println("🛒 Shop page accessed");
        System.out.println("Query params - Search: " + search + ", Category: " + category + ", Sort: " + sort);

        // Get sort option
        Sort sortOption = getSort(sort);
        Pageable pageable = PageRequest.of(page - 1, size, sortOption);

        try {
            // Get products
            Page<Product> productPage = productService.searchProducts(search, category, minPrice, maxPrice, pageable);

            // Prices are now automatically calculated by Product Entity lifecycle hooks

            model.addAttribute("products", productPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", productPage.getTotalPages());
            model.addAttribute("totalItems", productPage.getTotalElements());
            System.out.println(
                    "📄 PAGE=" + page + " | TOTAL_PRODUCTS=" + productPage.getTotalElements() + " | TOTAL_PAGES="
                            + productPage.getTotalPages() + " | ON_THIS_PAGE=" + productPage.getContent().size());

            // Add filter data
            model.addAttribute("categories", CATEGORIES);
            model.addAttribute("selectedCategory", category);
            model.addAttribute("searchQuery", search);
            model.addAttribute("currentSort", sort);

            // Price range for slider
            Double dbMinPrice = productService.getMinPrice();
            Double dbMaxPrice = productService.getMaxPrice();
            model.addAttribute("minPriceLimit", dbMinPrice != null ? dbMinPrice : 0);
            model.addAttribute("maxPriceLimit", dbMaxPrice != null ? dbMaxPrice : 1000);

            // User data
            if (principal != null) {
                User user = userService.getUserFromPrincipal(principal).orElse(null);

                if (user != null) {
                    model.addAttribute("user", user);
                    int cartCount = cartService.getCartItemCount(user.getId());
                    model.addAttribute("cartCount", cartCount);
                } else {
                    model.addAttribute("cartCount", 0);
                }
            } else {
                model.addAttribute("cartCount", 0);
            }

        } catch (Exception e) {
            System.out.println("❌ Error loading shop page: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading products: " + e.getMessage());
        }

        return "shop";
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model, Principal principal) {
        try {
            Product product = productService.getProductById(id).orElse(null);
            if (product == null) {
                model.addAttribute("error", "Product not found");
                return "redirect:/shop";
            }

            // Prices are now automatically calculated by Product Entity lifecycle hooks

            model.addAttribute("product", product);

            // Get current user
            if (principal != null) {
                User user = userService.getUserFromPrincipal(principal).orElse(null);

                if (user != null) {
                    model.addAttribute("user", user);

                    // Get cart count
                    int cartCount = cartService.getCartItemCount(user.getId());
                    model.addAttribute("cartCount", cartCount);

                    // Check if product is in wishlist
                    boolean inWishlist = productService.isProductInWishlist(user, product);
                    model.addAttribute("inWishlist", inWishlist);
                } else {
                    model.addAttribute("cartCount", 0);
                }
            } else {
                model.addAttribute("cartCount", 0);
            }

            // Get related products
            List<Product> relatedProducts = productService.getRelatedProducts(product.getCategory(), product.getId());
            // Prices are now automatically calculated by Product Entity lifecycle hooks
            model.addAttribute("relatedProducts", relatedProducts);

            // Get product reviews
            List<Review> reviews = reviewService.getProductReviews(id);
            model.addAttribute("reviews", reviews);

        } catch (Exception e) {
            model.addAttribute("error", "Error loading product details: " + e.getMessage());
            return "redirect:/shop";
        }

        return "product-details"; // Corrected view name
    }

    @GetMapping("/shop/category/{category}")
    public String shopByCategory(@PathVariable String category, Model model, Principal principal) {
        return shopPage(model, null, category, "newest", null, null, 1, 12, principal);
    }

    @GetMapping("/shop/search")
    public String searchProducts(@RequestParam String q, Model model, Principal principal) {
        return shopPage(model, q, null, "newest", null, null, 1, 12, principal);
    }

    @GetMapping("/api/categories")
    @ResponseBody
    public Map<String, Object> getCategories() {
        Map<String, Object> response = new HashMap<>();
        response.put("categories", CATEGORIES);
        response.put("success", true);
        return response;
    }

    @GetMapping("/api/filters")
    @ResponseBody
    public Map<String, Object> getFilterOptions() {
        Map<String, Object> filters = new HashMap<>();

        // Price ranges
        Map<String, Double> priceRanges = new HashMap<>();
        priceRanges.put("min", productService.getMinPrice());
        priceRanges.put("max", productService.getMaxPrice());
        filters.put("priceRanges", priceRanges);

        // Popular categories
        Map<String, Long> categoryCounts = productService.getCategoryCounts();
        filters.put("categoryCounts", categoryCounts);

        return filters;
    }

    @GetMapping("/api/autocomplete")
    @ResponseBody
    public List<String> autocomplete(@RequestParam String term) {
        return productService.searchProductNames(term);
    }

    private Sort getSort(String sort) {
        // Always add id as a stable secondary sort so OFFSET-based pagination is
        // deterministic even when the primary sort column contains NULLs
        Sort secondary = Sort.by("id").ascending();
        switch (sort) {
            case "price_low":
                return Sort.by("price").ascending().and(secondary);
            case "price_high":
                return Sort.by("price").descending().and(secondary);
            case "name_asc":
                return Sort.by("name").ascending().and(secondary);
            case "name_desc":
                return Sort.by("name").descending().and(secondary);
            case "rating":
                return Sort.by("averageRating").descending().and(secondary);
            case "popular":
                return Sort.by("salesCount").descending().and(secondary);
            case "discount":
                return Sort.by("discount").descending().and(secondary);
            default: // newest - fall back to id DESC if createdAt is NULL for old rows
                return Sort.by(Sort.Order.desc("id"));
        }
    }
}