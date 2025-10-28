package com.stockmate.order.common.consumer;

import com.stockmate.order.api.order.dto.StockDeductionSuccessEvent;
import com.stockmate.order.api.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockDeductionSuccessConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${kafka.topics.stock-deduction-success}",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleStockDeductionSuccess(StockDeductionSuccessEvent event) {
        log.info("=== 재고 차감 성공 이벤트 수신 === Order ID: {}, Order Number: {}", 
                event.getOrderId(), event.getOrderNumber());

        try {
            orderService.handleStockDeductionSuccessWebSocket(event);
            
            log.info("=== 재고 차감 성공 이벤트 처리 완료 === Order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("=== 재고 차감 성공 이벤트 처리 실패 === Order ID: {}, 에러: {}", event.getOrderId(), e.getMessage(), e);
        }
    }
}
