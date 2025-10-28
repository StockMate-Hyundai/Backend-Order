package com.stockmate.order.common.consumer;

import com.stockmate.order.api.order.dto.StockDeductionFailedEvent;
import com.stockmate.order.api.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockDeductionFailedConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${kafka.topics.stock-deduction-failed}",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleStockDeductionFailed(StockDeductionFailedEvent event) {
        log.info("=== 재고 차감 실패 이벤트 수신 === Order ID: {}, Order Number: {}, Reason: {}", 
                event.getOrderId(), event.getOrderNumber(), event.getReason());

        try {
            orderService.handleStockDeductionFailedWebSocket(event);
            
            log.info("=== 재고 차감 실패 이벤트 처리 완료 (주문 상태를 ORDER_COMPLETED로 되돌림) === Order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("=== 재고 차감 실패 이벤트 처리 실패 === Order ID: {}, 에러: {}", event.getOrderId(), e.getMessage(), e);
        }
    }
}
