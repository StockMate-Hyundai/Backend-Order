package com.stockmate.order.api.order.service;

import com.stockmate.order.api.order.dto.PayCancelRequestEvent;
import com.stockmate.order.api.order.dto.PayCancelResponseEvent;
import com.stockmate.order.api.order.dto.PayRequestEvent;
import com.stockmate.order.api.order.dto.PayResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final WebClient webClient;

    @Value("${payment.server.url}")
    private String paymentServerUrl;

    public PayResponseEvent requestDepositPay(PayRequestEvent payRequestEvent) {
        log.info("[PaymentService] 결제 요청 시작 - orderId: {}", payRequestEvent.getOrderId());
        try {
            PayResponseEvent response = webClient.post()
                    .uri(paymentServerUrl + "/api/v1/payment/pay")
                    .bodyValue(payRequestEvent)
                    .retrieve()
                    .bodyToMono(PayResponseEvent.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            log.info("✅ [PaymentService] 결제 API 호출 성공 - orderId: {}, response: {}", payRequestEvent.getOrderId(), response);

            return response;
        } catch (Exception e) {
            log.error("❌ [PaymentService] 결제 API 호출 실패 - orderId: {}, error: {}", payRequestEvent.getOrderId(), e.getMessage(), e);
            return null;
        }
    }

    public PayCancelResponseEvent requestDepositPayCancel(PayCancelRequestEvent payCancelRequestEvent) {
        log.info("[PaymentService] 결제 취소 요청 시작 - orderId: {}", payCancelRequestEvent.getOrderId());
        try {
            PayCancelResponseEvent response = webClient.post()
                    .uri(paymentServerUrl + "/api/v1/payment/cancel")
                    .bodyValue(payCancelRequestEvent)
                    .retrieve()
                    .bodyToMono(PayCancelResponseEvent.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            log.info("✅ [PaymentService] 결제 취소 API 호출 성공 - orderId: {}, response: {}", payCancelRequestEvent.getOrderId(), response);
            return response;
        } catch (Exception e) {
            log.error("❌ [PaymentService] 결제 API 호출 실패 - orderId: {}, error: {}", payCancelRequestEvent.getOrderId(), e.getMessage(), e);
            return null;
        }
    }
}
