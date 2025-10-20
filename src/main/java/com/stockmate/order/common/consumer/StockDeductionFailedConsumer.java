package com.stockmate.order.common.consumer;

import com.stockmate.order.api.order.dto.StockDeductionFailedEvent;
import com.stockmate.order.api.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
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
    public void handleStockDeductionFailed(@Payload StockDeductionFailedEvent event, Acknowledgment acknowledgment) {
        log.info("재고 차감 실패 이벤트 수신 - Order ID: {}, Order Number: {}, Reason: {}", 
                event.getOrderId(), event.getOrderNumber(), event.getReason());

        try {
            orderService.handleStockDeductionFailed(event);
            acknowledgment.acknowledge();
            log.info("재고 차감 실패 이벤트 처리 완료 (주문 반려) - Order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("재고 차감 실패 이벤트 처리 실패 - Order ID: {}, 에러: {}",
                    event.getOrderId(), e.getMessage(), e);
            // 재처리를 위해 acknowledge 하지 않음
        }
    }
}
