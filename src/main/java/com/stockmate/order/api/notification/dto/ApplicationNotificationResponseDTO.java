package com.stockmate.order.api.notification.dto;

import com.stockmate.order.api.notification.entity.ApplicationNotification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ApplicationNotificationResponseDTO {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private String message;
    private Boolean isRead;

    public static ApplicationNotificationResponseDTO of (ApplicationNotification a) {
        return ApplicationNotificationResponseDTO.builder()
                .id(a.getId())
                .orderId(a.getOrderId().getOrderId())
                .orderNumber(a.getOrderNumber())
                .message(a.getMessage())
                .isRead(a.getIsRead())
                .build();
    }
}