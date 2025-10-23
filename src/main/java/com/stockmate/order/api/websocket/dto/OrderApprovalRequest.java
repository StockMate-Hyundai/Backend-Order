package com.stockmate.order.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderApprovalRequest {
    private String type; // "ORDER_APPROVAL"
    private Long orderId;
    private String sessionId; // WebSocket 세션 ID
}
