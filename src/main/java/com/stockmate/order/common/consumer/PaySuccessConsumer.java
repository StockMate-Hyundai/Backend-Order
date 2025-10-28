package com.stockmate.order.common.consumer;

import com.stockmate.order.api.order.dto.payResponseEvent;
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
public class PaySuccessConsumer {
    private final OrderService orderService;

    @KafkaListener(
            topics = "${kafka.topics.pay-success}",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaySuccess(
            @Payload payResponseEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("결제 성공 이벤트 수신 - 토픽: {}, 파티션: {}, 오프셋: {}, Order ID: {}",
                topic, partition, offset, event.getOrderId());

        orderService.changeOrderStatus(event.getOrderId(), "PENDING_SHIPPING"); // 출고 대기로 변경
        acknowledgment.acknowledge();

        log.info("결제 성공 이벤트 처리 완료 (주문 상태를 PENDING_SHIPPING으로 변경) - Order ID: {}", event.getOrderId());
    }
}
