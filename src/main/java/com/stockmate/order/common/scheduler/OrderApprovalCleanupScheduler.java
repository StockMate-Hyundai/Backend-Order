package com.stockmate.order.common.scheduler;

import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderApprovalCleanupScheduler {

    private final OrderRepository orderRepository;
    private static final int APPROVAL_EXPIRY_MINUTES = 30; // 30분 이상 PENDING_APPROVAL인 경우 만료

    /**
     * 매 5분마다 만료된 PENDING_APPROVAL 주문을 PAY_COMPLETED로 되돌림
     * (Kafka 이벤트가 영원히 안 오는 경우 방지)
     */
    @Scheduled(fixedDelay = 300000) // 5분마다 실행 (300,000ms)
    @Transactional
    public void cleanupExpiredPendingApprovals() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(APPROVAL_EXPIRY_MINUTES);
        
        List<Order> expiredOrders = orderRepository.findExpiredPendingApprovals(
                OrderStatus.PENDING_APPROVAL, 
                expiryTime
        );

        if (expiredOrders.isEmpty()) {
            log.debug("만료된 PENDING_APPROVAL 주문 없음");
            return;
        }

        log.warn("만료된 PENDING_APPROVAL 주문 발견 - 개수: {}, 만료 기준: {}분 이전", 
                expiredOrders.size(), APPROVAL_EXPIRY_MINUTES);

        for (Order order : expiredOrders) {
            log.warn("만료된 주문 복원 시작 - Order ID: {}, Order Number: {}, 승인 시작 시간: {}", 
                    order.getOrderId(), order.getOrderNumber(), order.getApprovalStartedAt());

            // 주문 상태를 PAY_COMPLETED로 되돌림
            order.rollbackToPayCompleted();
            orderRepository.save(order);

            log.warn("만료된 주문 복원 완료 - Order ID: {}, Status: PAY_COMPLETED", order.getOrderId());
            
            // TODO: 관리자 알림 또는 모니터링 지표 전송
        }

        log.warn("만료된 PENDING_APPROVAL 주문 정리 완료 - 처리된 주문 수: {}", expiredOrders.size());
    }
}

