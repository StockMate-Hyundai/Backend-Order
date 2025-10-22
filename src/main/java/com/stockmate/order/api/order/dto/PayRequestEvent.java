package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayRequestEvent {
    private Long orderId;
    private String orderNumber;
    private String paymentType;
    private int totalPrice;
}
