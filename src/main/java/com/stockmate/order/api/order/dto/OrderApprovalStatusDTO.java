package com.stockmate.order.api.order.dto;

import com.stockmate.order.api.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderApprovalStatusDTO {
    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    private LocalDateTime updatedAt;
}