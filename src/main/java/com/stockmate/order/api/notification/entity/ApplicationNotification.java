package com.stockmate.order.api.notification.entity;

import com.stockmate.order.api.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApplicationNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order orderId;

    private Long userId;
    private String orderNumber;
    private String message;
    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    public void markAsRead() {
        this.isRead = true;
    }
}