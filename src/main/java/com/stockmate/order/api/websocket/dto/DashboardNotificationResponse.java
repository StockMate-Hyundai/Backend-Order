package com.stockmate.order.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardNotificationResponse {
    private String type; // "DASHBOARD_NOTIFICATION"
    private Long notificationId; // 알림 ID (읽음 처리 등에 사용)
    private String message; // "SMO-123 신규 주문이 발생하였습니다." 또는 "SMO-123 신규 주문 승인이 발생하였습니다."
    private DashboardData data; // orderId, orderNumber

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardData {
        private Long orderId;
        private String orderNumber;
    }
}

