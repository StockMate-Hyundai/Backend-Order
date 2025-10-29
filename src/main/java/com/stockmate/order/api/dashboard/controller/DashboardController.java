package com.stockmate.order.api.dashboard.controller;

import com.stockmate.order.api.dashboard.dto.TodayDashboardResponseDTO;
import com.stockmate.order.api.dashboard.service.DashboardService;
import com.stockmate.order.common.response.ApiResponse;
import com.stockmate.order.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "대시보드 관련 API 입니다.")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "금일 대시보드 조회",
            description = "금일 주문 수, 배송 처리 수, 배송 중인 수, 매출 금액 및 시간대별 추이를 조회합니다."
    )
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TodayDashboardResponseDTO>> getTodayDashboard() {
        log.info("금일 대시보드 조회 요청");

        TodayDashboardResponseDTO response = dashboardService.getTodayDashboard();

        log.info("금일 대시보드 조회 완료 - 총 주문: {}, 매출: {}", 
                response.getSummary().getTotalOrders(), 
                response.getSummary().getTotalRevenue());

        return ApiResponse.success(SuccessStatus.GET_TODAY_DASHBOARD_SUCCESS, response);
    }
}

