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
    
    // orderNumber로 주문 조회 (OrderItems와 함께 fetch join)
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);
    
    // 대시보드: 금일 주문 수 (PAY_COMPLETED 상태만 집계)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = 'PAY_COMPLETED' AND o.createdAt >= :startOfDay AND o.createdAt < :endOfDay")
    long countTodayOrders(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // 대시보드: 금일 배송 처리된 수 (SHIPPING 상태로 변경된 수)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = 'SHIPPING' AND o.updatedAt >= :startOfDay AND o.updatedAt < :endOfDay")
    long countTodayShippingProcessed(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // 대시보드: 금일 배송 중인 수 (현재 SHIPPING 상태)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = 'SHIPPING' AND o.createdAt >= :startOfDay AND o.createdAt < :endOfDay")
    long countTodayShippingInProgress(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // 대시보드: 금일 매출 (PAY_COMPLETED 상태의 총 금액)
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.orderStatus = 'PAY_COMPLETED' AND o.createdAt >= :startOfDay AND o.createdAt < :endOfDay")
    long calculateTodayRevenue(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // 대시보드: 시간대별 주문 수 (PAY_COMPLETED 상태만 집계)
    @Query("SELECT HOUR(o.createdAt), COUNT(o) FROM Order o WHERE o.orderStatus = 'PAY_COMPLETED' AND o.createdAt >= :startOfDay AND o.createdAt < :endOfDay GROUP BY HOUR(o.createdAt) ORDER BY HOUR(o.createdAt)")
    List<Object[]> countOrdersByHour(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // 대시보드: 시간대별 배송 처리 수
    @Query("SELECT HOUR(o.updatedAt), COUNT(o) FROM Order o WHERE o.orderStatus = 'SHIPPING' AND o.updatedAt >= :startOfDay AND o.updatedAt < :endOfDay GROUP BY HOUR(o.updatedAt) ORDER BY HOUR(o.updatedAt)")
    List<Object[]> countShippingProcessedByHour(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // 대시보드: 시간대별 배송 중인 수
    @Query("SELECT HOUR(o.createdAt), COUNT(o) FROM Order o WHERE o.orderStatus = 'SHIPPING' AND o.createdAt >= :startOfDay AND o.createdAt < :endOfDay GROUP BY HOUR(o.createdAt) ORDER BY HOUR(o.createdAt)")
    List<Object[]> countShippingInProgressByHour(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // 대시보드: 시간대별 매출
    @Query("SELECT HOUR(o.createdAt), COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.orderStatus = 'PAY_COMPLETED' AND o.createdAt >= :startOfDay AND o.createdAt < :endOfDay GROUP BY HOUR(o.createdAt) ORDER BY HOUR(o.createdAt)")
    List<Object[]> calculateRevenueByHour(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // 대시보드: 카테고리별 판매량
    @Query("""
        SELECT oi.categoryName, SUM(oi.amount)
        FROM Order o
        JOIN o.orderItems oi
        WHERE o.orderStatus = 'PAY_COMPLETED'
        AND o.createdAt >= :startOfDay AND o.createdAt < :endOfDay
        GROUP BY oi.categoryName
        ORDER BY SUM(oi.amount) DESC
    """)
    List<Object[]> getCategorySalesByDate(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // 대시보드: TOP 판매 부품 (부품명+카테고리 기준, 판매량 상위)
    @Query("""
        SELECT oi.name, oi.categoryName, SUM(oi.amount)
        FROM Order o
        JOIN o.orderItems oi
        WHERE o.orderStatus = 'PAY_COMPLETED'
        AND o.createdAt >= :startOfDay AND o.createdAt < :endOfDay
        GROUP BY oi.name, oi.categoryName
        ORDER BY SUM(oi.amount) DESC
    """)
    List<Object[]> getTopPartsByDate(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // 대시보드: 최근 주문 이력 (지정된 날짜, 최대 10개, 모든 상태)
    @Query("""
        SELECT o.orderId, o.createdAt, o.orderNumber, o.totalPrice, o.memberId,
               (SELECT SUM(oi.amount) FROM OrderItem oi WHERE oi.order.orderId = o.orderId)
        FROM Order o
        WHERE o.createdAt >= :startOfDay AND o.createdAt < :endOfDay
        ORDER BY o.createdAt DESC
    """)
    List<Object[]> getRecentOrdersByDate(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}
