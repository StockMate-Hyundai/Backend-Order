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
import java.util.List;

@Tag(name = "Order", description = "주문 관련 API 입니다.")
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성 API", description = "본사에 있는 부품들을 발주합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<MakeOrderResponseDto>> makeOrder(@RequestBody OrderRequestDTO orderRequestDTO, @AuthenticationPrincipal SecurityUser securityUser) {
        log.info("부품 발주 요청 - 요청 가맹점 ID: {}, 주문 항목 수: {}", securityUser.getMemberId(), orderRequestDTO.getOrderItems().size());

        MakeOrderResponseDto response = orderService.makeOrder(orderRequestDTO, securityUser.getMemberId());
        log.info("부품 발주 완료 - Order ID: {}, Order Number: {}", response.getOrderId(), response.getOrderNumber());

        return ApiResponse.success(SuccessStatus.SEND_PARTS_ORDER_SUCCESS, response);
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

    @Operation(summary = "주문 반려 API", description = "주문을 반려합니다. (ADMIN/SUPER_ADMIN만 가능)")
    @PutMapping("/reject")
    public ResponseEntity<ApiResponse<Void>> requestOrderReject(@RequestBody OrderRejectRequestDTO orderRejectRequestDTO, @AuthenticationPrincipal SecurityUser securityUser) {

        log.info("주문 반려 요청 - Order ID: {}, 요청자 ID: {}, 요청자 Role: {}", orderRejectRequestDTO.getOrderId(), securityUser.getMemberId(), securityUser.getRole());

        orderService.requestOrderReject(orderRejectRequestDTO, securityUser.getRole());

        log.info("주문 반려 요청 완료 - Order ID: {}", orderRejectRequestDTO.getOrderId());

        return ApiResponse.success_only(SuccessStatus.SEND_ORDER_REJECT_REQUEST_SUCCESS);
    }

    @Operation(summary = "주문 승인 요청 API (WebSocket)", description = "주문 승인 처리를 시작합니다. WebSocket 연결을 통해 실시간으로 처리 상태를 받을 수 있습니다. (ADMIN/SUPER_ADMIN만 가능)")
    @PutMapping("/approve")
    public ResponseEntity<ApiResponse<Void>> requestOrderApproval(@RequestParam Long orderId, @AuthenticationPrincipal SecurityUser securityUser) {

        log.info("주문 승인 요청 (WebSocket) - Order ID: {}, 요청자 ID: {}, 요청자 Role: {}", orderId, securityUser.getMemberId(), securityUser.getRole());

        // 주문 승인 처리 시작 (WebSocket으로 결과 전송)
        orderService.requestOrderApprovalWebSocket(orderId, securityUser.getRole(), securityUser.getMemberId());
        log.info("주문 승인 요청 접수 완료 - Order ID: {}", orderId);

        return ApiResponse.success_only(SuccessStatus.SEND_ORDER_APPROVAL_REQUEST_SUCCESS);
    }

    @Operation(summary = "주문 승인 상태 체크 API", description = "주문의 현재 상태를 확인합니다. (본인 주문 또는 ADMIN/SUPER_ADMIN)")
    @GetMapping("/approval/status")
    public ResponseEntity<ApiResponse<OrderApprovalStatusDTO>> checkOrderApprovalStatus(
            @RequestParam Long orderId,
            @AuthenticationPrincipal SecurityUser securityUser) {

        log.info("주문 승인 상태 체크 요청 - Order ID: {}, 요청자 ID: {}",
                orderId, securityUser.getMemberId());

        OrderApprovalStatusDTO status = orderService.checkOrderApprovalStatus(
                orderId,
                securityUser.getMemberId(),
                securityUser.getRole()
        );

        log.info("주문 승인 상태 체크 완료 - Order ID: {}, Status: {}", orderId, status.getStatus());

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

    @Operation(summary = "주문 검증 조회 API", description = "주문 ID로 검증 데이터를 조회합니다.")
    @GetMapping("validate/{orderId}")
    public ResponseEntity<ApiResponse<OrderValidateDTO>> getValidateOrder(@PathVariable Long orderId, @AuthenticationPrincipal SecurityUser securityUser) {
        log.info("주문 검증 조회 요청 - Order ID: {}, 요청자 ID: {}", orderId, securityUser.getMemberId());

        OrderValidateDTO response = orderService.getValidateOrder(orderId, securityUser.getMemberId());
        return ApiResponse.success(SuccessStatus.CHECK_ORDER_DATA_SUCCESS, response);
    }

    @Operation(summary = "출고 대기 변경 API", description = "창고 관리자가 주문 상태를 PENDING_SHIPPING으로 변경합니다. WAREHOUSE 역할만 가능합니다.")
    @PutMapping("/pending-shipping/{orderId}")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatusToPendingShipping(@PathVariable Long orderId, @AuthenticationPrincipal SecurityUser securityUser) {

        log.info("주문 상태 출고 대기 변경 요청 - Order ID: {}, 요청자 ID: {}, 요청자 Role: {}", orderId, securityUser.getMemberId(), securityUser.getRole());

        orderService.updateOrderStatusToPendingShipping(orderId, securityUser.getRole());
        log.info("주문 상태 출고 대기 변경 완료 - Order ID: {}", orderId);

        return ApiResponse.success_only(SuccessStatus.UPDATE_ORDER_STATUS_TO_PENDING_SHIPPING_SUCCESS);
    }

    @Operation(summary = "배송 등록 API", description = "주문에 배송 정보를 등록합니다. WAREHOUSE 역할만 가능합니다.")
    @PostMapping("/shipping")
    public ResponseEntity<ApiResponse<ShippingRegistrationResponseDTO>> registerShipping(@RequestBody ShippingRegistrationRequestDTO requestDTO, @AuthenticationPrincipal SecurityUser securityUser) {

        log.info("배송 등록 요청 - Order Number: {}, 요청자 ID: {}, 요청자 Role: {}", requestDTO.getOrderNumber(), securityUser.getMemberId(), securityUser.getRole());

        ShippingRegistrationResponseDTO shippingInfo = orderService.registerShipping(requestDTO, securityUser.getRole());
        log.info("배송 등록 완료 - Order Number: {}, Tracking Number: {}", requestDTO.getOrderNumber(), shippingInfo.getTrackingNumber());

        return ApiResponse.success(SuccessStatus.REGISTER_SHIPPING_SUCCESS, shippingInfo);
    }

    @Operation(summary = "입고 처리 요청 API", description = "주문에 입고 처리를 요청합니다. 가맹점 사용자만 가능합니다.")
    @PostMapping("/receive")
    public ResponseEntity<ApiResponse<Void>> requestReceivingProcess(@RequestBody ReceivingProcessRequestDTO requestDTO, @AuthenticationPrincipal SecurityUser securityUser) {

        log.info("입고 처리 요청 - Order Number: {}, 요청자 ID: {}, 요청자 Role: {}", requestDTO.getOrderNumber(), securityUser.getMemberId(), securityUser.getRole());

        orderService.requestReceivingProcess(requestDTO, securityUser.getRole(), securityUser.getMemberId());
        log.info("입고 처리 요청 완료 - Order Number: {}", requestDTO.getOrderNumber());

        return ApiResponse.success(SuccessStatus.REQUEST_RECEIVING_PROCESS_SUCCESS, null);
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

    @Operation(summary = "카테고리별 지출 금액 조회 API", description = "저번달의 카테고리별 지출 금액을 조회합니다.")
    @GetMapping("/category-spend")
    public ResponseEntity<ApiResponse<List<CategorySpendingDto>>> getMonthlyCategorySpending(
//            @AuthenticationPrincipal SecurityUser securityUser
    ) {
//        List<CategorySpendingDto> response = orderService.getMonthlyCategorySpending(securityUser.getMemberId());
        List<CategorySpendingDto> response = orderService.getMonthlyCategorySpending(9L);
        return ApiResponse.success(SuccessStatus.GET_MONTHLY_SPEND_SUCCESS, response);
    }
  
    @Operation(summary = "네비게이션용 부품 정보 조회 API", description = "주문 번호 리스트로 부품 위치 정보를 조회합니다.")
    @PostMapping("/navigation/parts")
    public ResponseEntity<ApiResponse<NavigationPartsResponseDTO>> getPartsForNavigation(@RequestBody NavigationPartsRequestDTO requestDTO) {

        log.info("네비게이션용 부품 정보 조회 요청 - 주문 번호 수: {}", requestDTO.getOrderNumbers().size());
        NavigationPartsResponseDTO response = orderService.getPartsForNavigation(requestDTO.getOrderNumbers());

        log.info("네비게이션용 부품 정보 조회 완료 - 총 부품 위치 수: {}", response.getPartLocations().size());
        return ApiResponse.success(SuccessStatus.SEND_NAVIGATION_PARTS_SUCCESS, response);
    }

}