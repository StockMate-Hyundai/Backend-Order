package com.stockmate.order.api.notification.entity;

import com.stockmate.order.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dashboard_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DashboardNotification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType; // ADMIN, WAREHOUSE

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String orderNumber;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 알림을 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
    }
}

