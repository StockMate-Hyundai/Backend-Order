package com.stockmate.order.common.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmate.order.api.order.dto.ReceivingHistoryFailedEvent;
import com.stockmate.order.api.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReceivingHistoryFailedConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.receiving-history-failed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleReceivingHistoryFailed(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("입고 히스토리 등록 실패 이벤트 수신 - 토픽: {}, 파티션: {}, 오프셋: {}, Payload: {}",
                topic, partition, offset, payload);

        try {
            // Map을 ReceivingHistoryFailedEvent로 변환
            ReceivingHistoryFailedEvent event = objectMapper.convertValue(payload, ReceivingHistoryFailedEvent.class);
            
            log.info("입고 히스토리 등록 실패 이벤트 변환 완료 - Order ID: {}, Order Number: {}, Error: {}", 
                    event.getOrderId(), event.getOrderNumber(), event.getErrorMessage());

            // 입고 히스토리 등록 실패 처리
            orderService.handleReceivingHistoryFailedWebSocket(event);
            acknowledgment.acknowledge();
            
            log.info("입고 히스토리 등록 실패 이벤트 처리 완료 - Order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("입고 히스토리 등록 실패 이벤트 처리 실패 - 에러: {}", e.getMessage(), e);
            acknowledgment.acknowledge(); // 실패해도 acknowledge하여 무한 재시도 방지
        }
    }
}