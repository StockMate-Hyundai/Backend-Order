package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateResponseDTO {
    private Long orderId;
    private String orderNumber;
    private int totalPrice;
    private String orderStatus;
}
