package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelResponseEvent {
    private Long orderId;
    private String orderNumber;
    private String approvalAttemptId; // Saga 시도 식별자

    public static CancelResponseEvent of (CancelRequestEvent c) {
        return CancelResponseEvent.builder()
                .orderId(c.getOrderId())
                .orderNumber(c.getOrderNumber())
                .approvalAttemptId("CANCEL-" + System.currentTimeMillis())
                .build();
    }
}
