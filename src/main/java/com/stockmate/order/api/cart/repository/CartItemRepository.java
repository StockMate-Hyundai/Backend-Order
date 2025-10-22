package com.stockmate.order.api.cart.repository;

import com.stockmate.order.api.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}

