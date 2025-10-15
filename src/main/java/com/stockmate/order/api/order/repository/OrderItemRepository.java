package com.stockmate.order.api.order.repository;

import com.stockmate.order.api.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
