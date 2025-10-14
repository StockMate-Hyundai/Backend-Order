package com.stockmate.order.api.health.controller;

import com.stockmate.order.common.response.ApiResponse;
import com.stockmate.order.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "HealthCheck", description = "HealthCheck 관련 API 입니다.")
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class HealthCheckController {

    @Operation(
            summary = "Health Check API"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "서버 상태 체크 성공"),
    })
    @GetMapping("/health-check")
    public ResponseEntity<ApiResponse<Void>> healthCheck() {

        return ApiResponse.success_only(SuccessStatus.SEND_HEALTH_CHECK_SUCCESS);
    }
}