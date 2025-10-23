package com.stockmate.order.api.websocket.dto;

import com.stockmate.order.api.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderApprovalResponse {
    private String type; // "ORDER_APPROVAL_RESPONSE"
    private Long orderId;
    private OrderStatus status;
    private String message;
    private String step; // "STOCK_CHECK", "STOCK_DEDUCTION", "PAYMENT", "COMPLETED", "FAILED"
    private Object data; // 추가 데이터 (에러 정보 등)
}
