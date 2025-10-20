package com.stockmate.order.common.consumer;

import com.stockmate.order.api.order.dto.StockDeductionFailedEvent;
import com.stockmate.order.api.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockDeductionFailedConsumer {

    private final OrderService orderService;
    
    // 재시도 횟수를 추적하기 위한 맵 (Order ID -> 재시도 횟수)
    private final ConcurrentHashMap<Long, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();
    private static final int MAX_RETRY_COUNT = 3; // 최대 재시도 횟수

    @KafkaListener(
            topics = "${kafka.topics.stock-deduction-failed}",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleStockDeductionFailed(
            @Payload StockDeductionFailedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("재고 차감 실패 이벤트 수신 - 토픽: {}, 파티션: {}, 오프셋: {}, Order ID: {}, Order Number: {}, Reason: {}", 
                topic, partition, offset, event.getOrderId(), event.getOrderNumber(), event.getReason());

        try {
            orderService.handleStockDeductionFailed(event);
            acknowledgment.acknowledge();
            
            // 처리 성공 시 재시도 카운트 제거
            retryCountMap.remove(event.getOrderId());
            
            log.info("재고 차감 실패 이벤트 처리 완료 (주문 상태를 ORDER_COMPLETED로 되돌림) - Order ID: {}", event.getOrderId());
        } catch (Exception e) {
            // 재시도 횟수 증가
            AtomicInteger retryCount = retryCountMap.computeIfAbsent(event.getOrderId(), k -> new AtomicInteger(0));
            int currentRetryCount = retryCount.incrementAndGet();
            
            log.error("재고 차감 실패 이벤트 처리 실패 - Order ID: {}, 재시도 횟수: {}/{}, 에러: {}",
                    event.getOrderId(), currentRetryCount, MAX_RETRY_COUNT, e.getMessage(), e);
            
            // 최대 재시도 횟수 초과 시 acknowledge 처리 (DLQ로 이동하거나 무시)
            if (currentRetryCount >= MAX_RETRY_COUNT) {
                log.error("최대 재시도 횟수 초과 - Order ID: {}를 acknowledge 처리하여 더 이상 재시도하지 않음", 
                        event.getOrderId());
                acknowledgment.acknowledge();
                retryCountMap.remove(event.getOrderId());
            }
            // 최대 재시도 횟수 미달 시 acknowledge 하지 않아 재시도
        }
    }
}
