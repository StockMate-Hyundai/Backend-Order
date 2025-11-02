package com.stockmate.order.api.notification.dto;

import com.stockmate.order.api.notification.entity.DashboardNotification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationListResponseDTO {
    private Long id;
    private String message;
    private Long orderId;
    private String orderNumber;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationListResponseDTO from(DashboardNotification notification) {
        return NotificationListResponseDTO.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .orderId(notification.getOrderId())
                .orderNumber(notification.getOrderNumber())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

