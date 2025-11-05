package com.stockmate.order.api.notification.controller;

import com.stockmate.order.api.notification.entity.ApplicationNotification;
import com.stockmate.order.api.notification.service.ApplicationNotificationService;
import com.stockmate.order.common.config.security.SecurityUser;
import com.stockmate.order.common.response.ApiResponse;
import com.stockmate.order.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "지점 알림 API", description = "지점 알림 조회 및 관리")
@RestController
@RequestMapping("/api/v1/order/store/notifications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationNotificationController {
    private final ApplicationNotificationService applicationNotificationService;

    @Operation(summary = "알림 전체 조회")
    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<ApplicationNotification>>> getNotifications(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        List<ApplicationNotification> response = applicationNotificationService.getNotification(securityUser.getMemberId());
        return ApiResponse.success(SuccessStatus.GET_ALL_STORE_NOTIFICATION, response);
    }

    @Operation(summary = "읽지 않은 알림 조회")
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<ApplicationNotification>>> getUnread(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        List<ApplicationNotification> response = applicationNotificationService.getUnreadNotifications(securityUser.getMemberId());
        return ApiResponse.success(SuccessStatus.GET_UNREAD_STORE_NOTIFICATION, response);
    }

    @Operation(summary = "읽지 않은 알림 개수 조회")
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Long response = applicationNotificationService.getUnreadCount(securityUser.getMemberId());
        return ApiResponse.success(SuccessStatus.GET_UNREAD_STORE_COUNT, response);
    }

    @Operation(summary = "알림 읽음 처리")
    @PatchMapping("/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @RequestParam Long notificationId
    ) {
        applicationNotificationService.markAsRead(notificationId);
        return ApiResponse.success_only(SuccessStatus.SET_UNREAD_CHANGE);
    }

    @Operation(summary = "전체 알림 읽음 처리")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        applicationNotificationService.markAllAsRead(securityUser.getMemberId());
        return ApiResponse.success_only(SuccessStatus.SET_ALL_UNREAD_CHANGE);
    }
}
