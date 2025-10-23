package com.stockmate.order.api.order.repository;

import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {
    
    // OrderItems와 함께 fetch join으로 로드 (LazyInitializationException 방지)
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);
    
    // 만료된 PENDING_APPROVAL 주문 조회 (스케줄러용)
    @Query("SELECT o FROM Order o WHERE o.orderStatus = :status AND o.approvalStartedAt < :expiryTime")
    List<Order> findExpiredPendingApprovals(@Param("status") OrderStatus status, @Param("expiryTime") LocalDateTime expiryTime);
    
    // orderNumber로 주문 조회
    Optional<Order> findByOrderNumber(String orderNumber);
}
