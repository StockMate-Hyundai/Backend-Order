package com.stockmate.order.common.producer;

import com.stockmate.order.api.order.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.stock-deduction-request}")
    private String stockDeductionRequestTopic;

    @Value("${kafka.topics.stock-restore-request}")
    private String stockRestoreRequestTopic;

    @Value("${kafka.topics.pay-request}")
    private String payRequestTopic;

    // 재고 차감 요청 이벤트 발송
    public void sendStockDeductionRequest(StockDeductionRequestEvent event) {
        log.info("재고 차감 요청 이벤트 발송 시작 - Order ID: {}, Order Number: {}", 
                event.getOrderId(), event.getOrderNumber());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                stockDeductionRequestTopic,
                event.getOrderId().toString(),
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("재고 차감 요청 이벤트 발송 성공 - 토픽: {}, 파티션: {}, 오프셋: {}, Order ID: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getOrderId());
            } else {
                log.error("재고 차감 요청 이벤트 발송 실패 - Order ID: {}, 에러: {}",
                        event.getOrderId(), ex.getMessage(), ex);
            }
        });
    }

    // 재고 복구 요청 이벤트 발송 (보상 트랜잭션)
    public void sendStockRestoreRequest(StockRestoreRequestEvent event) {
        log.info("재고 복구 요청 이벤트 발송 시작 - Order ID: {}, Order Number: {}, Reason: {}", 
                event.getOrderId(), event.getOrderNumber(), event.getReason());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                stockRestoreRequestTopic,
                event.getOrderId().toString(),
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("재고 복구 요청 이벤트 발송 성공 - 토픽: {}, 파티션: {}, 오프셋: {}, Order ID: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getOrderId());
            } else {
                log.error("재고 복구 요청 이벤트 발송 실패 - Order ID: {}, 에러: {}",
                        event.getOrderId(), ex.getMessage(), ex);
            }
        });
    }

    // 결제 요청 이벤트 발송
    public void sendPayRequest(PayRequestEvent event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                payRequestTopic,
                event.getOrderId().toString(),
                event
        );

        future.whenComplete((result,  ex) -> {
            if (ex == null) {
                log.info("결제 요청 이벤트 발송 성공 - 토픽: {}, 파티션: {}, 오프셋: {}, Order ID: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getOrderId());
            } else {
                log.error("결제 요청 이벤트 발송 실패 - Order ID: {}, 에러: {}",
                        event.getOrderId(), ex.getMessage(), ex);
            }
        });
    }
}
