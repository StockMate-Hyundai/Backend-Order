package com.stockmate.order.api.notification.repository;

import com.stockmate.order.api.notification.entity.ApplicationNotification;
import com.stockmate.order.api.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicationNotificationRepository extends JpaRepository<ApplicationNotification, Long> {
    // 알림 조회
    List<ApplicationNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 읽지 않은 알림 갯수
    Long countByUserIdAndIsReadFalse(Long userId);

    // 읽지 않은 알림 조회
    List<ApplicationNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    // 모든 알림을 읽음 처리 (Bulk Update)
    @Modifying
    @Query("""
        UPDATE ApplicationNotification a
        SET a.isRead = true
        WHERE a.userId = :userId
          AND a.isRead = false
        ORDER BY a.createdAt
    """)
    void markAllAsReadByUserId(@Param("userId") Long userId);
}
