package com.company.I_SPICE.repository;

import com.company.I_SPICE.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByFeaturedTrue();

    List<Product> findByCategory(String category);

    // Autocomplete
    List<Product> findByNameContainingIgnoreCase(String name);

    // Filter limits
    @Query("SELECT MIN(p.price) FROM Product p")
    Double findMinPrice();

    @Query("SELECT MAX(p.price) FROM Product p")
    Double findMaxPrice();

    // Category counts
    @Query("SELECT p.category, COUNT(p) FROM Product p GROUP BY p.category")
    List<Object[]> countProductsByCategory();
}