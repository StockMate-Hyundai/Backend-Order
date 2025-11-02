package com.stockmate.order.api.notification.controller;

import com.stockmate.order.api.notification.dto.NotificationListResponseDTO;
import com.stockmate.order.api.notification.dto.UnreadCountResponseDTO;
import com.stockmate.order.api.notification.entity.DashboardNotification;
import com.stockmate.order.api.notification.entity.NotificationType;
import com.stockmate.order.api.notification.service.DashboardNotificationService;
import com.stockmate.order.common.response.ApiResponse;
import com.stockmate.order.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "대시보드 알림 API", description = "관리자/창고관리자 알림 조회 및 관리")
@RestController
@RequestMapping("/api/v1/order/notifications")
@RequiredArgsConstructor
@Slf4j
public class DashboardNotificationController {

    private final DashboardNotificationService notificationService;

    @Operation(summary = "읽지 않은 알림 조회", description = "특정 타입(admin/warehouse)의 읽지 않은 알림을 조회합니다.")
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationListResponseDTO>>> getUnreadNotifications(@RequestParam String type) {
        
        log.info("읽지 않은 알림 조회 요청 - Type: {}", type);
        
        NotificationType notificationType = parseType(type);
        List<DashboardNotification> notifications = notificationService.getUnreadNotifications(notificationType);
        
        List<NotificationListResponseDTO> response = notifications.stream()
                .map(NotificationListResponseDTO::from)
                .collect(Collectors.toList());
        
        log.info("읽지 않은 알림 조회 완료 - Type: {}, Count: {}", type, response.size());
        return ApiResponse.success(SuccessStatus.GET_UNREAD_NOTIFICATIONS_SUCCESS, response);
    }

    @Operation(summary = "모든 알림 조회", description = "특정 타입(admin/warehouse)의 모든 알림을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationListResponseDTO>>> getAllNotifications(@RequestParam String type) {
        
        log.info("모든 알림 조회 요청 - Type: {}", type);
        
        NotificationType notificationType = parseType(type);
        List<DashboardNotification> notifications = notificationService.getAllNotifications(notificationType);
        
        List<NotificationListResponseDTO> response = notifications.stream()
                .map(NotificationListResponseDTO::from)
                .collect(Collectors.toList());
        
        log.info("모든 알림 조회 완료 - Type: {}, Count: {}", type, response.size());
        return ApiResponse.success(SuccessStatus.GET_ALL_NOTIFICATIONS_SUCCESS, response);
    }

    @Operation(summary = "읽지 않은 알림 개수 조회", description = "특정 타입(admin/warehouse)의 읽지 않은 알림 개수를 조회합니다.")
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<UnreadCountResponseDTO>> getUnreadCount(
            @RequestParam String type) {
        
        log.info("읽지 않은 알림 개수 조회 요청 - Type: {}", type);
        
        NotificationType notificationType = parseType(type);
        long count = notificationService.getUnreadCount(notificationType);
        
        UnreadCountResponseDTO response = UnreadCountResponseDTO.builder()
                .unreadCount(count)
                .build();
        
        log.info("읽지 않은 알림 개수 조회 완료 - Type: {}, Count: {}", type, count);
        return ApiResponse.success(SuccessStatus.GET_UNREAD_COUNT_SUCCESS, response);
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    @PutMapping("/read/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long notificationId) {
        
        log.info("알림 읽음 처리 요청 - Notification ID: {}", notificationId);
        notificationService.markAsRead(notificationId);
        
        log.info("알림 읽음 처리 완료 - Notification ID: {}", notificationId);
        return ApiResponse.success_only(SuccessStatus.MARK_NOTIFICATION_READ_SUCCESS);
    }

    @Operation(summary = "모든 알림 읽음 처리", description = "특정 타입(admin/warehouse)의 모든 알림을 읽음 처리합니다.")
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@RequestParam String type) {
        
        log.info("모든 알림 읽음 처리 요청 - Type: {}", type);
        
        NotificationType notificationType = parseType(type);
        int count = notificationService.markAllAsRead(notificationType);
        
        log.info("모든 알림 읽음 처리 완료 - Type: {}, Count: {}", type, count);
        return ApiResponse.success_only(SuccessStatus.MARK_ALL_NOTIFICATIONS_READ_SUCCESS);
    }

    /**
     * 타입 문자열을 NotificationType enum으로 변환
     */
    private NotificationType parseType(String type) {
        try {
            return NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("유효하지 않은 타입 - Type: {}", type);
            throw new IllegalArgumentException("유효하지 않은 타입입니다. admin 또는 warehouse만 허용됩니다.");
        }
    }
}

