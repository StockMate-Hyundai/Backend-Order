package com.stockmate.order.api.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayCancelResponseEvent {
    private Long orderId;
    private String orderNumber;
    private String approvalAttemptId; // Saga 시도 식별자
    @JsonProperty("success")   // 또는 "isSuccess"
    private Boolean isSuccess;
    private String message;

    public static PayCancelResponseEvent of(PayCancelRequestEvent event, Boolean isSuccess, String message) {
        return PayCancelResponseEvent.builder()
                .orderId(event.getOrderId())
                .orderNumber(event.getOrderNumber())
                .approvalAttemptId("CANCEL-" + System.currentTimeMillis())
                .isSuccess(isSuccess)
                .message(message)
                .build();
    }
}