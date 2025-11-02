package com.stockmate.order.api.notification.service;

import com.stockmate.order.api.notification.entity.DashboardNotification;
import com.stockmate.order.api.notification.entity.NotificationType;
import com.stockmate.order.api.notification.repository.DashboardNotificationRepository;
import com.stockmate.order.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardNotificationService {

    private final DashboardNotificationRepository notificationRepository;

    // 알림 저장
    @Transactional
    public DashboardNotification saveNotification(NotificationType type, String message, Long orderId, String orderNumber) {
        DashboardNotification notification = DashboardNotification.builder()
                .notificationType(type)
                .message(message)
                .orderId(orderId)
                .orderNumber(orderNumber)
                .isRead(false)
                .build();

        DashboardNotification saved = notificationRepository.save(notification);
        log.info("알림 저장 완료 - ID: {}, Type: {}, Order ID: {}", saved.getId(), type, orderId);
        return saved;
    }

    // 특정 타입의 읽지 않은 알림 조회
    @Transactional(readOnly = true)
    public List<DashboardNotification> getUnreadNotifications(NotificationType type) {
        return notificationRepository.findByNotificationTypeAndIsReadFalseOrderByCreatedAtDesc(type);
    }

    // 특정 타입의 모든 알림 조회
    @Transactional(readOnly = true)
    public List<DashboardNotification> getAllNotifications(NotificationType type) {
        return notificationRepository.findByNotificationTypeOrderByCreatedAtDesc(type);
    }

    // 특정 타입의 읽지 않은 알림 개수 조회
    @Transactional(readOnly = true)
    public long getUnreadCount(NotificationType type) {
        return notificationRepository.countByNotificationTypeAndIsReadFalse(type);
    }

    // 특정 알림을 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId) {
        DashboardNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("알림을 찾을 수 없습니다. ID: " + notificationId));

        notification.markAsRead();
        log.info("알림 읽음 처리 완료 - ID: {}", notificationId);
    }

    // 특정 타입의 모든 알림을 읽음 처리
    @Transactional
    public int markAllAsRead(NotificationType type) {
        int count = notificationRepository.markAllAsReadByType(type);
        log.info("알림 전체 읽음 처리 완료 - Type: {}, Count: {}", type, count);
        return count;
    }
}

