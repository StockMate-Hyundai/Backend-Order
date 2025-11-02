package com.stockmate.order.api.notification.repository;

import com.stockmate.order.api.notification.entity.DashboardNotification;
import com.stockmate.order.api.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DashboardNotificationRepository extends JpaRepository<DashboardNotification, Long> {

    // 특정 타입의 읽지 않은 알림 조회 (최신순)
    List<DashboardNotification> findByNotificationTypeAndIsReadFalseOrderByCreatedAtDesc(NotificationType notificationType);

    // 특정 타입의 모든 알림 조회 (최신순)
    List<DashboardNotification> findByNotificationTypeOrderByCreatedAtDesc(NotificationType notificationType);

    // 특정 타입의 읽지 않은 알림 개수 조회
    long countByNotificationTypeAndIsReadFalse(NotificationType notificationType);

    // 특정 타입의 모든 알림을 읽음 처리 (Bulk Update)
    @Modifying
    @Query("UPDATE DashboardNotification n SET n.isRead = true WHERE n.notificationType = :notificationType AND n.isRead = false")
    int markAllAsReadByType(@Param("notificationType") NotificationType notificationType);
}

