package com.company.I_SPICE.repository;

import com.company.I_SPICE.model.Cart;
import com.company.I_SPICE.model.CartItem;
import com.company.I_SPICE.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    void deleteByCart(Cart cart);
    int countByCart(Cart cart);
}