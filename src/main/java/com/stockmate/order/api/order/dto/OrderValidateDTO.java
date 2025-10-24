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
public class OrderValidateDTO {
    private Long orderId;
    private String orderNumber;
    private PaymentType paymentType;
    private int totalPrice;
    private OrderStatus orderStatus;

    public static OrderValidateDTO of(Order o) {
        return OrderValidateDTO.builder()
                .orderId(o.getOrderId())
                .orderNumber(o.getOrderNumber())
                .paymentType(o.getPaymentType())
                .totalPrice(o.getTotalPrice())
                .orderStatus(o.getOrderStatus())
                .build();
    }
}

