package com.stockmate.order.api.order.controller;

import com.stockmate.order.api.order.dto.*;
import com.stockmate.order.api.order.entity.OrderStatus;
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

import java.time.LocalDate;

@Tag(name = "Order", description = "주문 관련 API 입니다.")
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성 API", description = "본사에 있는 부품들을 발주합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> makeOrder(@RequestBody OrderRequestDTO orderRequestDTO, @AuthenticationPrincipal SecurityUser securityUser) {
        log.info("부품 발주 요청 - 요청 가맹점 ID: {}, 주문 항목 수: {}", securityUser.getMemberId(), orderRequestDTO.getOrderItems().size());

        orderService.makeOrder(orderRequestDTO, securityUser.getMemberId());
        return ApiResponse.success_only(SuccessStatus.SEND_PARTS_ORDER_SUCCESS);
    }

    @Operation(summary = "주문 승인 요청 API", description = "주문을 승인하고 재고 차감을 요청합니다. (ADMIN/SUPER_ADMIN만 가능)")
    @PutMapping("/{orderId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveOrder(@PathVariable Long orderId, @AuthenticationPrincipal SecurityUser securityUser) {
        
        log.info("주문 승인 요청 - Order ID: {}, 요청자 ID: {}, 요청자 Role: {}", 
                orderId, securityUser.getMemberId(), securityUser.getRole());

        orderService.requestOrderApproval(orderId, securityUser.getRole());
        
        log.info("주문 승인 요청 완료 - Order ID: {} (재고 차감 처리 중)", orderId);
        
        return ApiResponse.success_only(SuccessStatus.SEND_ORDER_APPROVAL_REQUEST_SUCCESS);
    }

    @Operation(summary = "주문 취소 API", description = "생성한 주문을 취소합니다. (본인 주문 또는 ADMIN/SUPER_ADMIN)")
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long orderId, @AuthenticationPrincipal SecurityUser securityUser) {
        
        log.info("주문 취소 요청 - 요청 가맹점 ID: {}, 취소 주문 ID: {}, Role: {}", securityUser.getMemberId(), orderId, securityUser.getRole());
        orderService.cancelOrder(orderId, securityUser.getMemberId(), securityUser.getRole());
        
        log.info("주문 취소 완료 - Order ID: {}", orderId);
        return ApiResponse.success_only(SuccessStatus.SEND_CANCELLED_ORDER_SUCCESS);
    }

    @Operation(summary = "주문 물리적 삭제 API", description = "주문을 DB에서 완전히 삭제합니다. (ADMIN/SUPER_ADMIN만 가능)")
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long orderId, @AuthenticationPrincipal SecurityUser securityUser) {
        
        log.info("주문 물리적 삭제 요청 - Order ID: {}, 요청자 ID : {}, 요청자 Role: {}", orderId, securityUser.getMemberId(), securityUser.getRole());
        orderService.deleteOrder(orderId, securityUser);
        
        log.info("주문 물리적 삭제 완료 - Order ID: {}", orderId);
        return ApiResponse.success_only(SuccessStatus.DELETE_ORDER_SUCCESS);
    }

    @Operation(summary = "주문 승인 요청 API", description = "주문을 승인합니다. (ADMIN/SUPER_ADMIN만 가능)")
    @PutMapping("/approve")
    public ResponseEntity<ApiResponse<Void>> requestOrderApproval(@RequestParam Long orderId, @AuthenticationPrincipal SecurityUser securityUser) {
        
        log.info("주문 승인 요청 - Order ID: {}, 요청자 ID: {}, 요청자 Role: {}", 
                orderId, securityUser.getMemberId(), securityUser.getRole());

        orderService.requestOrderApproval(orderId, securityUser.getRole());
        
        log.info("주문 승인 요청 완료 - Order ID: {}", orderId);
        
        return ApiResponse.success_only(SuccessStatus.SEND_ORDER_APPROVAL_REQUEST_SUCCESS);
    }

    @Operation(summary = "주문 승인 상태 체크 API", description = "주문의 현재 상태를 확인합니다. (본인 주문 또는 ADMIN/SUPER_ADMIN)")
    @GetMapping("/approval/status")
    public ResponseEntity<ApiResponse<OrderStatus>> checkOrderApprovalStatus(@RequestParam Long orderId, @AuthenticationPrincipal SecurityUser securityUser) {
        
        log.info("주문 승인 상태 체크 요청 - Order ID: {}, 요청자 ID: {}", 
                orderId, securityUser.getMemberId());

        OrderStatus status = orderService.checkOrderApprovalStatus(
                orderId, 
                securityUser.getMemberId(), 
                securityUser.getRole()
        );
        
        log.info("주문 승인 상태 체크 완료 - Order ID: {}, Status: {}", orderId, status);
        
        return ApiResponse.success(SuccessStatus.CHECK_ORDER_APPROVAL_STATUS_SUCCESS, status);
    }

    @Operation(summary = "주문 상세 조회 API", description = "주문 ID로 주문 상세 정보를 조회합니다. (본인 주문 또는 ADMIN/SUPER_ADMIN)")
    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<OrderDetailResponseDTO>> getOrderDetail(@RequestParam Long orderId, @AuthenticationPrincipal SecurityUser securityUser) {
        
        log.info("주문 상세 조회 요청 - Order ID: {}, 요청자 ID: {}, 요청자 Role: {}", orderId, securityUser.getMemberId(), securityUser.getRole());
        OrderDetailResponseDTO response = orderService.getOrderDetail(orderId, securityUser.getMemberId(), securityUser.getRole());
        
        log.info("주문 상세 조회 완료 - Order ID: {}, Order Number: {}", orderId, response.getOrderNumber());
        return ApiResponse.success(SuccessStatus.SEND_ORDER_DETAIL_SUCCESS, response);
    }

    @Operation(summary = "내 주문 리스트 조회 API", description = "내가 생성한 주문 리스트를 조회합니다.")
    @GetMapping("/list/my")
    public ResponseEntity<ApiResponse<OrderListResponseDTO>> getMyOrderList(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal SecurityUser securityUser) {
        
        log.info("내 주문 리스트 조회 요청 - Member ID: {}", securityUser.getMemberId());

        MyOrderListRequestDTO requestDTO = MyOrderListRequestDTO.builder()
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .build();

        OrderListResponseDTO response = orderService.getMyOrderList(requestDTO, securityUser.getMemberId());
        
        log.info("내 주문 리스트 조회 완료 - 총 주문 수: {}", response.getTotalElements());
        return ApiResponse.success(SuccessStatus.SEND_MY_ORDER_LIST_SUCCESS, response);
    }

    @Operation(summary = "주문 리스트 조회 API (관리자용)", description = "필터링을 통해 주문 리스트를 조회합니다. (ADMIN/SUPER_ADMIN만 가능)")
    @PostMapping("/list")
    public ResponseEntity<ApiResponse<OrderListResponseDTO>> getOrderList(@RequestBody OrderListRequestDTO orderListRequestDTO, @AuthenticationPrincipal SecurityUser securityUser) {
        
        log.info("주문 리스트 조회 요청 - 요청자 ID: {}, 요청자 Role: {}", securityUser.getMemberId(), securityUser.getRole());
        OrderListResponseDTO response = orderService.getOrderList(orderListRequestDTO, securityUser.getRole());
        
        log.info("주문 리스트 조회 완료 - 총 주문 수: {}", response.getTotalElements());
        return ApiResponse.success(SuccessStatus.SEND_ORDER_LIST_SUCCESS, response);
    }

}
