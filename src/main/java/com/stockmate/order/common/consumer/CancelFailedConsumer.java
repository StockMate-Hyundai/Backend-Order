package com.stockmate.order.common.consumer;

import com.stockmate.order.api.order.dto.CancelResponseEvent;
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
public class CancelFailedConsumer {
    private final OrderService orderService;

    @KafkaListener(
            topics = "${kafka.topics.cancel-failed}",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCancelFailed(
            @Payload CancelResponseEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("결제 취소 실패 이벤트 수신 - 토픽: {}, 파티션: {}, 오프셋: {}, Order ID: {}, Order Number: {}",
                topic, partition, offset, event.getOrderId(), event.getOrderNumber());

        orderService.changeOrderStatus(event.getOrderId(), "REFUND_REJECTED");
        acknowledgment.acknowledge();

        log.info("결제 취소 실패 이벤트 처리 완료 (주문 상태를 REFUND_REJECTED로 변경) - Order ID: {}", event.getOrderId());
    }
}
