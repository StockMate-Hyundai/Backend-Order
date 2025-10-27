package com.stockmate.order.common.consumer;

import com.stockmate.order.api.order.dto.ReceivingHistorySuccessEvent;
import com.stockmate.order.api.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReceivingHistorySuccessConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${kafka.topics.receiving-history-success}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleReceivingHistorySuccess(
            @Payload ReceivingHistorySuccessEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("입고 히스토리 등록 성공 이벤트 수신 - 토픽: {}, 파티션: {}, 오프셋: {}, Order ID: {}, Order Number: {}",
                topic, partition, offset, event.getOrderId(), event.getOrderNumber());

        // 입고 히스토리 등록 성공 처리
        orderService.handleReceivingHistorySuccessWebSocket(event);
        acknowledgment.acknowledge();
        
        log.info("입고 히스토리 등록 성공 이벤트 처리 완료 - Order ID: {}", event.getOrderId());
    }
}
