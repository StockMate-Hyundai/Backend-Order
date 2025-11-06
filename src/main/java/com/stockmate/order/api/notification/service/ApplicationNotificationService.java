package com.stockmate.order.api.notification.service;

import com.stockmate.order.api.notification.dto.ApplicationNotificationResponseDTO;
import com.stockmate.order.api.notification.entity.ApplicationNotification;
import com.stockmate.order.api.notification.entity.DashboardNotification;
import com.stockmate.order.api.notification.entity.NotificationType;
import com.stockmate.order.api.notification.repository.ApplicationNotificationRepository;
import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationNotificationService {
    private final ApplicationNotificationRepository applicationNotificationRepository;

    // 알림 저장
    @Transactional
    public ApplicationNotification saveNotification(Order orderId, String orderNumber, String message, Long userId) {
        log.info("[Notification][SAVE] 요청 - orderId={}, orderNumber={}, userId={}, message={}",
                orderId != null ? orderId.getOrderId() : null,
                orderNumber, userId, message);

        ApplicationNotification notification = ApplicationNotification.builder()
                .orderId(orderId)
                .userId(userId)
                .orderNumber(orderNumber)
                .message(message)
                .isRead(false)
                .build();

        ApplicationNotification saved = applicationNotificationRepository.save(notification);

        log.info("[Notification][SAVE] 완료 - notificationId={}", saved.getId());
        return saved;
    }

    // 알림 조회
    public List<ApplicationNotificationResponseDTO> getNotification(Long userId) {
        log.info("[Notification][GET] 요청 - userId={}", userId);
        List<ApplicationNotification> data = applicationNotificationRepository.findByUserId(userId);

        List<ApplicationNotificationResponseDTO> result = data.stream()
                .map(ApplicationNotificationResponseDTO::of)
                .toList();

        log.info("[Notification][GET] 조회결과 - count={}", data.size());
        return result;
    }

    // 읽지 않은 알림 조회
    public List<ApplicationNotificationResponseDTO> getUnreadNotifications(Long userId) {
        log.info("[Notification][GET-UNREAD] 요청 - userId={}", userId);
        List<ApplicationNotification> data = applicationNotificationRepository.findByUserIdAndIsReadFalse(userId);

        List<ApplicationNotificationResponseDTO> result = data.stream()
                        .map(ApplicationNotificationResponseDTO::of)
                        .toList();

        log.info("[Notification][GET-UNREAD] 조회결과 - count={}", data.size());
        return result;
    }

    // 읽지 않은 알림 개수 조회
    public Long getUnreadCount(Long userId) {
        log.info("[Notification][GET-UNREAD-COUNT] 요청 - userId={}", userId);
        Long count = applicationNotificationRepository.countByUserIdAndIsReadFalse(userId);

        log.info("[Notification][GET-UNREAD-COUNT] 조회결과 - count={}", count);
        return count;
    }

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId) {
        log.info("[Notification][MARK-READ] 요청 - notificationId={}", notificationId);
        ApplicationNotification notification = applicationNotificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.warn("[Notification][MARK-READ] 실패 → notificationId={} 존재하지 않음", notificationId);
                    return new NotFoundException("알림을 찾을 수 없습니다.");
                });
        notification.markAsRead();
        log.info("[Notification][MARK-READ] 완료 - notificationId={}", notificationId);
    }

    // 모든 알림 읽음 처리
    @Transactional
    public void markAllAsRead(Long userID) {
        applicationNotificationRepository.markAllAsReadByUserId(userID);
        log.info("알림 전체 읽음 처리 완료");
    }
}
