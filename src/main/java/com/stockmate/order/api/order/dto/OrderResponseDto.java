package com.stockmate.order.api.order.dto;

import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderItem;
import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.order.entity.PaymentType;
import com.stockmate.order.common.entity.BaseTimeEntity;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDto extends BaseTimeEntity {
    private Long orderId;
    private String orderNumber;
    private int totalPrice;
    private PaymentType paymentType;
    private Boolean isSuccess;
    private String message;

    public static OrderResponseDto of(Order o, Boolean isSuccess, String message) {
        return OrderResponseDto.builder()
                .orderId(o.getOrderId())
                .orderNumber(o.getOrderNumber())
                .totalPrice(o.getTotalPrice())
                .paymentType(o.getPaymentType())
                .isSuccess(isSuccess)
                .message(message)
                .build();
    }
}
