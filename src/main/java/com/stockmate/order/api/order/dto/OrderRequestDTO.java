package com.stockmate.order.api.order.dto;

import com.stockmate.order.api.order.entity.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDTO {

    private List<OrderItemRequestDTO> orderItems;
    private LocalDate requestedShippingDate;
    private PaymentType paymentType;
    private String etc;
}
