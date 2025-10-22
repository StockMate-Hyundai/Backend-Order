package com.stockmate.order.api.order.dto;

import com.stockmate.order.api.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderApprovalResponseDTO {
    private Long orderId;
    private String orderNumber;
    private OrderStatus currentStatus;
    private String message;
}