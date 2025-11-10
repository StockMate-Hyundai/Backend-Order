package com.stockmate.order.api.order.dto;

import com.stockmate.order.api.order.entity.Order;
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
public class PayCancelRequestEvent {
    private Long orderId;
    private Long memberId;
    private String orderNumber;
    private PaymentType paymentType;
    private int totalPrice;
    private OrderStatus orderStatus;

    public static PayCancelRequestEvent of (Order o, Long memberId) {
        return PayCancelRequestEvent.builder()
                .orderId(o.getOrderId())
                .memberId(memberId)
                .orderNumber(o.getOrderNumber())
                .paymentType(o.getPaymentType())
                .totalPrice(o.getTotalPrice())
                .orderStatus(o.getOrderStatus())
                .build();
    }
}
