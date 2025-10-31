package com.stockmate.order.api.order.repository;

import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface OrderRepositoryCustom {
    Page<Order> findOrdersWithFilters(
            OrderStatus status,
            Long partId,
            Long memberId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );
    
    Page<Order> findOrdersWithFilters(
            OrderStatus status,
            Long partId,
            Long memberId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable,
            boolean excludeFailed
    );
}
