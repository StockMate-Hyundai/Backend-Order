package com.stockmate.order.api.order.controller;

import com.stockmate.order.api.order.dto.OrderRequestDTO;
import com.stockmate.order.api.order.service.OrderService;
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

@Tag(name = "Order", description = "주문 관련 API 입니다.")
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성 API", description = "본사에 있는 부품을 발주합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> makeOrder(@RequestBody OrderRequestDTO orderRequestDTO, @AuthenticationPrincipal SecurityUser securityUser) {
        log.info("부품 발주 요청 - 요청 가맹점 ID: {}, Parts ID: {}, 수량 : {}", securityUser.getMemberId(), orderRequestDTO.getPartsId(), orderRequestDTO.getAmount());

        orderService.makeOrder(orderRequestDTO, securityUser.getMemberId());
        return ApiResponse.success_only(SuccessStatus.SEND_PARTS_ORDER_SUCCESS);
    }
}
