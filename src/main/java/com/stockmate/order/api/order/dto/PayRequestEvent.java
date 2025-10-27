package com.stockmate.order.api.order.dto;

import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.order.entity.PaymentType;
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
    private PaymentType paymentType;
    private int totalPrice;
    private OrderStatus orderStatus;
}
