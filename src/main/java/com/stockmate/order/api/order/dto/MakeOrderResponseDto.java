package com.stockmate.order.api.order.dto;

import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.PaymentType;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MakeOrderResponseDto {
    private Long orderId;
    private String orderNumber;
    private int totalPrice;
    private PaymentType paymentType;

    public static MakeOrderResponseDto of(Order o) {
        return MakeOrderResponseDto.builder()
                .orderId(o.getOrderId())
                .orderNumber(o.getOrderNumber())
                .totalPrice(o.getTotalPrice())
                .paymentType(o.getPaymentType())
                .build();
    }
}
