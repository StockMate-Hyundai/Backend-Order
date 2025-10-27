package com.stockmate.order.common.consumer;

import com.stockmate.order.api.order.dto.ReceivingHistoryFailedEvent;
import com.stockmate.order.api.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReceivingHistoryFailedConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${kafka.topics.receiving-history-failed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleReceivingHistoryFailed(ReceivingHistoryFailedEvent event) {
        log.info("=== 입고 히스토리 등록 실패 이벤트 수신 === Order ID: {}, Order Number: {}, Error: {}", 
                event.getOrderId(), event.getOrderNumber(), event.getErrorMessage());

        try {
            // 입고 히스토리 등록 실패 처리
            orderService.handleReceivingHistoryFailedWebSocket(event);
            
            log.info("=== 입고 히스토리 등록 실패 이벤트 처리 완료 === Order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("=== 입고 히스토리 등록 실패 이벤트 처리 실패 === 에러: {}", e.getMessage(), e);
        }
    }
}