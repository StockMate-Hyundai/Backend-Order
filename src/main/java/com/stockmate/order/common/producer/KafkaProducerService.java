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

    @Value("${kafka.topics.pay-request}")
    private String payRequestTopic;

    @Value("${kafka.topics.cancel-request}")
    private String cancelRequestTopic;

    @Value("${kafka.topics.receiving-process-request}")
    private String receivingProcessRequestTopic;

    @Value("${kafka.topics.receiving-history-request}")
    private String receivingHistoryRequestTopic;

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

    // 결체 취소 요청 이벤트 발송
    public void sendCancelRequest(CancelRequestEvent event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                cancelRequestTopic,
                event.getOrderId().toString(),
                event
        );

        future.whenComplete((result,  ex) -> {
            if (ex == null) {
                log.info("결제 취소 요청 이벤트 발송 성공 - 토픽: {}, 파티션: {}, 오프셋: {}, Order ID: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getOrderId());
            } else {
                log.error("결제 취소 요청 이벤트 발송 실패 - Order ID: {}, 에러: {}",
                        event.getOrderId(), ex.getMessage(), ex);
            }
        });
    }

    // 입고 처리 요청 이벤트 발송
    public void sendReceivingProcessRequest(ReceivingProcessRequestEvent event) {
        log.info("입고 처리 요청 이벤트 발송 시작 - Order ID: {}, Order Number: {}, 가맹점 ID: {}", 
                event.getOrderId(), event.getOrderNumber(), event.getMemberId());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                receivingProcessRequestTopic,
                event.getOrderId().toString(),
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("입고 처리 요청 이벤트 발송 성공 - 토픽: {}, 파티션: {}, 오프셋: {}, Order ID: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getOrderId());
            } else {
                log.error("입고 처리 요청 이벤트 발송 실패 - Order ID: {}, 에러: {}",
                        event.getOrderId(), ex.getMessage(), ex);
            }
        });
    }

    // 입고 히스토리 등록 요청 이벤트 발송
    public void sendReceivingHistoryRequest(ReceivingHistoryRequestEvent event) {
        log.info("입고 히스토리 등록 요청 이벤트 발송 시작 - Order ID: {}, Order Number: {}, 가맹점 ID: {}", 
                event.getOrderId(), event.getOrderNumber(), event.getMemberId());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                receivingHistoryRequestTopic,
                event.getOrderId().toString(),
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("입고 히스토리 등록 요청 이벤트 발송 성공 - 토픽: {}, 파티션: {}, 오프셋: {}, Order ID: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getOrderId());
            } else {
                log.error("입고 히스토리 등록 요청 이벤트 발송 실패 - Order ID: {}, 에러: {}",
                        event.getOrderId(), ex.getMessage(), ex);
            }
        });
    }
}
