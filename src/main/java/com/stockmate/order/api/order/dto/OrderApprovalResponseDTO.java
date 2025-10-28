package com.stockmate.order.api.order.dto;

import com.stockmate.order.api.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderApprovalResponseDTO {
    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    private String message;
    private boolean success;
}

