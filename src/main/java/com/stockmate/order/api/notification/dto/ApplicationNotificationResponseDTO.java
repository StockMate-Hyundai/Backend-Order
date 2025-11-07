package com.stockmate.order.api.notification.dto;

import com.stockmate.order.api.notification.entity.ApplicationNotification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class ApplicationNotificationResponseDTO {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private String message;
    private LocalDateTime createdAt;
    private boolean isRead;

    public static ApplicationNotificationResponseDTO of (ApplicationNotification a) {
        return ApplicationNotificationResponseDTO.builder()
                .id(a.getId())
                .orderId(a.getOrderId() != null ? a.getOrderId().getOrderId() : null)
                .orderNumber(a.getOrderNumber())
                .message(a.getMessage())
                .createdAt(a.getCreatedAt())
                .isRead(a.getIsRead())
                .build();
    }
}