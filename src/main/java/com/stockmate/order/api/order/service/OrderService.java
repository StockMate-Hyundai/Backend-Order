package com.stockmate.order.api.order.service;

import com.stockmate.order.api.order.dto.*;
import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderItem;
import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.order.repository.OrderRepository;
import com.stockmate.order.api.websocket.handler.OrderWebSocketHandler;
import com.stockmate.order.common.config.security.Role;
import com.stockmate.order.common.config.security.SecurityUser;
import com.stockmate.order.common.exception.BadRequestException;
import com.stockmate.order.common.exception.NotFoundException;
import com.stockmate.order.common.exception.UnauthorizedException;
import com.stockmate.order.common.response.ApiResponse;
import com.stockmate.order.common.response.ErrorStatus;
import com.stockmate.order.common.response.SuccessStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final UserService userService;
    private final com.stockmate.order.common.producer.KafkaProducerService kafkaProducerService;
    private final OrderTransactionService orderTransactionService;
    private final OrderWebSocketHandler orderWebSocketHandler;


    @Transactional
    public void makeOrder(OrderRequestDTO orderRequestDTO, Long memberId) {
        log.info("부품 발주 시작 - Member ID: {}, 주문 항목 수: {}",
                memberId, orderRequestDTO.getOrderItems().size());

        List<OrderItemCheckRequestDTO> checkItems = new ArrayList<>();
        for (OrderItemRequestDTO item : orderRequestDTO.getOrderItems()) {
            checkItems.add(OrderItemCheckRequestDTO.builder()
                    .partId(item.getPartId())
                    .amount(item.getAmount())
                    .build());
        }

        InventoryCheckResponseDTO checkResult = inventoryService.checkInventory(checkItems);
        log.info("재고 체크 완료 - 총 금액: {}", checkResult.getTotalPrice());

        Order order = Order.builder()
                .totalPrice(checkResult.getTotalPrice())
                .paymentType(orderRequestDTO.getPaymentType())
                .requestedShippingDate(orderRequestDTO.getRequestedShippingDate())
                .shippingDate(null)
                .carrier(null)
                .trackingNumber(null)
                .rejectedMessage(null)
                .orderStatus(OrderStatus.ORDER_COMPLETED)
                .etc(orderRequestDTO.getEtc())
                .memberId(memberId)
                .orderItems(new ArrayList<>())
                .build();

        for (OrderItemRequestDTO itemRequest : orderRequestDTO.getOrderItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .partId(itemRequest.getPartId())
                    .amount(itemRequest.getAmount())
                    .build();
            order.getOrderItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        String orderNumber = "SMO-" + savedOrder.getOrderId();
        Order updatedOrder = savedOrder.toBuilder()
                .orderNumber(orderNumber)
                .build();
        Order finalOrder = orderRepository.save(updatedOrder);

        log.info("부품 발주 완료 - Order ID: {}, Order Number: {}, Member ID: {}, 주문 항목 수: {}, 총 금액: {}, Status: {}",
                finalOrder.getOrderId(), finalOrder.getOrderNumber(), finalOrder.getMemberId(),
                finalOrder.getOrderItems().size(), checkResult.getTotalPrice(),
        PayRequestEvent payRequestEvent = PayRequestEvent.builder()
                .orderId(finalOrder.getOrderId())
                .orderNumber(finalOrder.getOrderNumber())
                .totalPrice(finalOrder.getTotalPrice())
                .build();

        kafkaProducerService.sendPayRequest(payRequestEvent);

        log.info("결제 요청 이벤트 발송 완료 - Order ID: {}, 금액: {}",
                finalOrder.getOrderId(), finalOrder.getTotalPrice());

        log.info("부품 발주 완료 - Order ID: {}, Order Number: {}, Member ID: {}, 주문 항목 수: {}, 총 금액: {}, Status: {}",
                finalOrder.getOrderId(), finalOrder.getOrderNumber(), finalOrder.getMemberId(),
                finalOrder.getOrderItems().size(), checkResult.getTotalPrice(),
                finalOrder.getOrderStatus());
    }

    @Transactional
    public void cancelOrder(Long orderId, Long memberId, Role role) {
        log.info("주문 취소 요청 - Order ID: {}, Member ID: {}, Role: {}", orderId, memberId, role);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", orderId);
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());
                });

        boolean isAdmin = role == Role.ADMIN || role == Role.SUPER_ADMIN;
        if (!isAdmin && !order.getMemberId().equals(memberId)) {
            log.error("권한 없음 - Order의 Member ID: {}, 요청자 Member ID: {}, Role: {}",
                    order.getMemberId(), memberId, role);
            throw new BadRequestException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            log.warn("이미 취소된 주문 - Order ID: {}", orderId);
            throw new BadRequestException(ErrorStatus.ALREADY_CANCELLED_ORDER_EXCEPTION.getMessage());
        }

        if (!isAdmin && (order.getOrderStatus() == OrderStatus.SHIPPING ||
                order.getOrderStatus() == OrderStatus.DELIVERED ||
                order.getOrderStatus() == OrderStatus.RECEIVED)) {
            log.warn("취소 불가능한 상태 - Order ID: {}, Status: {}", orderId, order.getOrderStatus());
            throw new BadRequestException(ErrorStatus.ALREADY_SHIPPED_OR_DELIVERED_ORDER_EXCEPTION.getMessage());
        }

        order.cancel();
        orderRepository.save(order);

        log.info("주문 취소 완료 - Order ID: {}, Order Number: {}, 취소자 Role: {}",
                orderId, order.getOrderNumber(), role);
    }

    @Transactional
    public void deleteOrder(Long orderId, SecurityUser securityUser) {
        Role role = securityUser.getRole();
        Long adminId = securityUser.getMemberId();

        log.info("주문 물리적 삭제 요청 - Order ID: {}, 요청자 ID: {}, 요청자 Role: {}", orderId, adminId, role);

        if (role != Role.ADMIN && role != Role.SUPER_ADMIN) {
            log.error("권한 부족 - Role: {}", role);
            throw new BadRequestException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", orderId);
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());
                });

        orderRepository.delete(order);

        log.info("주문 물리적 삭제 완료 - Order ID: {}, Order Number: {}, 관리자 ID: {}, 관리자 Role: {}",
                orderId, order.getOrderNumber(), adminId, role);
    }

    @Transactional(readOnly = true)
    public OrderListResponseDTO getOrderList(OrderListRequestDTO requestDTO, Role role) {
        log.info("주문 리스트 조회 - Status: {}, PartId: {}, MemberId: {}, StartDate: {}, EndDate: {}, Page: {}, Size: {}, Role: {}",
                requestDTO.getStatus(), requestDTO.getPartId(), requestDTO.getMemberId(),
                requestDTO.getStartDate(), requestDTO.getEndDate(), requestDTO.getPage(), requestDTO.getSize(), role);

        if (role != Role.ADMIN && role != Role.SUPER_ADMIN) {
            log.error("권한 부족 - Role: {}", role);
            throw new BadRequestException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        int page = requestDTO.getPage() < 0 ? 0 : requestDTO.getPage();
        int size = (requestDTO.getSize() <= 0 || requestDTO.getSize() > 200) ? 20 : requestDTO.getSize();
        Pageable pageable = PageRequest.of(page, size);

        Page<Order> orderPage = orderRepository.findOrdersWithFilters(
                requestDTO.getStatus(),
                requestDTO.getPartId(),
                requestDTO.getMemberId(),
                requestDTO.getStartDate(),
                requestDTO.getEndDate(),
                pageable
        );

        if (orderPage.isEmpty()) {
            log.info("주문 리스트가 비어있음");
            return OrderListResponseDTO.builder()
                    .totalElements(0)
                    .totalPages(0)
                    .page(page)
                    .size(size)
                    .isLast(true)
                    .content(new ArrayList<>())
                    .build();
        }

        Set<Long> memberIds = orderPage.getContent().stream()
                .map(Order::getMemberId)
                .collect(Collectors.toSet());

        Set<Long> partIds = orderPage.getContent().stream()
                .flatMap(order -> order.getOrderItems().stream())
                .map(OrderItem::getPartId)
                .collect(Collectors.toSet());

        log.info("외부 서버 호출 준비 - 사용자 수: {}, 부품 수: {}", memberIds.size(), partIds.size());

        Map<Long, UserBatchResponseDTO> userMap = userService.getUsersByMemberIds(new ArrayList<>(memberIds));
        Map<Long, PartDetailResponseDTO> partMap = inventoryService.getPartDetails(new ArrayList<>(partIds));

        log.info("외부 서버 호출 완료 - 조회된 사용자: {}, 조회된 부품: {}", userMap.size(), partMap.size());

        List<OrderDetailResponseDTO> content = orderPage.getContent().stream()
                .map(order -> toOrderDetailResponseDTO(order, userMap, partMap))
                .collect(Collectors.toList());

        OrderListResponseDTO response = OrderListResponseDTO.builder()
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .isLast(orderPage.isLast())
                .content(content)
                .build();

        log.info("주문 리스트 조회 완료 - 총 주문 수: {}, 현재 페이지 주문 수: {}",
                response.getTotalElements(), content.size());

        return response;
    }

    private OrderDetailResponseDTO toOrderDetailResponseDTO(
            Order order,
            Map<Long, UserBatchResponseDTO> userMap,
            Map<Long, PartDetailResponseDTO> partMap) {

        List<OrderItemDetailDTO> orderItemDetails = order.getOrderItems().stream()
                .map(item -> OrderItemDetailDTO.builder()
                        .partId(item.getPartId())
                        .amount(item.getAmount())
                        .partDetail(partMap.get(item.getPartId()))
                        .build())
                .collect(Collectors.toList());

        return OrderDetailResponseDTO.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .memberId(order.getMemberId())
                .userInfo(userMap.get(order.getMemberId()))
                .orderItems(orderItemDetails)
                .etc(order.getEtc())
                .rejectedMessage(order.getRejectedMessage())
                .carrier(order.getCarrier())
                .trackingNumber(order.getTrackingNumber())
                .requestedShippingDate(order.getRequestedShippingDate())
                .shippingDate(order.getShippingDate())
                .totalPrice(order.getTotalPrice())
                .orderStatus(order.getOrderStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponseDTO getOrderDetail(Long orderId, Long memberId, Role role) {
        log.info("주문 상세 조회 - Order ID: {}, 요청자 Member ID: {}, Role: {}", orderId, memberId, role);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", orderId);
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());});

        boolean isAdmin = role == Role.ADMIN || role == Role.SUPER_ADMIN;
        if (!isAdmin && !order.getMemberId().equals(memberId)) {
            log.error("권한 없음 - Order의 Member ID: {}, 요청자 Member ID: {}, Role: {}", order.getMemberId(), memberId, role);
            throw new BadRequestException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        List<Long> memberIds = List.of(order.getMemberId());
        Map<Long, UserBatchResponseDTO> userMap = userService.getUsersByMemberIds(memberIds);

        List<Long> partIds = order.getOrderItems().stream()
                .map(OrderItem::getPartId)
                .collect(Collectors.toList());
        Map<Long, PartDetailResponseDTO> partMap = inventoryService.getPartDetails(partIds);

        log.info("주문 상세 조회 완료 - Order ID: {}, Order Number: {}", orderId, order.getOrderNumber());

        return toOrderDetailResponseDTO(order, userMap, partMap);
    }

    @Transactional(readOnly = true)
    public OrderListResponseDTO getMyOrderList(MyOrderListRequestDTO requestDTO, Long memberId) {
        log.info("내 주문 리스트 조회 - Member ID: {}, Status: {}, StartDate: {}, EndDate: {}, Page: {}, Size: {}",
                memberId, requestDTO.getStatus(), requestDTO.getStartDate(), requestDTO.getEndDate(),
                requestDTO.getPage(), requestDTO.getSize());

        int page = requestDTO.getPage() < 0 ? 0 : requestDTO.getPage();
        int size = (requestDTO.getSize() <= 0 || requestDTO.getSize() > 200) ? 20 : requestDTO.getSize();
        Pageable pageable = PageRequest.of(page, size);

        Page<Order> orderPage = orderRepository.findOrdersWithFilters(
                requestDTO.getStatus(),
                null,
                memberId,
                requestDTO.getStartDate(),
                requestDTO.getEndDate(),
                pageable
        );

        if (orderPage.isEmpty()) {
            log.info("주문 리스트가 비어있음 - Member ID: {}", memberId);
            return OrderListResponseDTO.builder()
                    .totalElements(0)
                    .totalPages(0)
                    .page(page)
                    .size(size)
                    .isLast(true)
                    .content(new ArrayList<>())
                    .build();
        }

        Set<Long> partIds = orderPage.getContent().stream()
                .flatMap(order -> order.getOrderItems().stream())
                .map(OrderItem::getPartId)
                .collect(Collectors.toSet());

        log.info("외부 서버 호출 준비 - 부품 수: {}", partIds.size());

        Map<Long, UserBatchResponseDTO> userMap = userService.getUsersByMemberIds(List.of(memberId));
        Map<Long, PartDetailResponseDTO> partMap = inventoryService.getPartDetails(new ArrayList<>(partIds));

        log.info("외부 서버 호출 완료 - 조회된 부품: {}", partMap.size());

        List<OrderDetailResponseDTO> content = orderPage.getContent().stream()
                .map(order -> toOrderDetailResponseDTO(order, userMap, partMap))
                .collect(Collectors.toList());

        OrderListResponseDTO response = OrderListResponseDTO.builder()
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .isLast(orderPage.isLast())
                .content(content)
                .build();

        log.info("내 주문 리스트 조회 완료 - Member ID: {}, 총 주문 수: {}, 현재 페이지 주문 수: {}",
                memberId, response.getTotalElements(), content.size());

        return response;
    }

    @Transactional
    public void requestOrderReject(OrderRejectRequestDTO orderRejectRequestDTO, Role role) {
        log.info("주문 반려 요청 - Order ID: {}, Role: {}", orderRejectRequestDTO.getOrderId(), role);

        if (role != Role.ADMIN && role != Role.SUPER_ADMIN) {
            log.error("권한 부족 - Role: {}", role);
            throw new BadRequestException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        Order order = orderRepository.findById(orderRejectRequestDTO.getOrderId())
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", orderRejectRequestDTO.getOrderId());
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());
                });

        if (order.getOrderStatus() != OrderStatus.ORDER_COMPLETED) {
            log.warn("승인 불가능한 상태 - Order ID: {}, Status: {}", orderRejectRequestDTO.getOrderId(), order.getOrderStatus());
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS_FOR_APPROVAL.getMessage());
        }

        order.reject(orderRejectRequestDTO.getReason());
        orderRepository.save(order);

        log.info("주문 반려 완료 - Order ID: {}, Order Number: {}, Status: REJECTED", orderRejectRequestDTO.getOrderId(), order.getOrderNumber());
    }


    // 주문 승인 상태 체크
    @Transactional(readOnly = true)
    public OrderApprovalStatusDTO checkOrderApprovalStatus(Long orderId, Long memberId, Role role) {
        log.info("주문 승인 상태 체크 - Order ID: {}, Member ID: {}, Role: {}", orderId, memberId, role);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", orderId);
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());
                });

        boolean isAdmin = role == Role.ADMIN || role == Role.SUPER_ADMIN;
        if (!isAdmin && !order.getMemberId().equals(memberId)) {
            log.error("권한 없음 - Order의 Member ID: {}, 요청자 Member ID: {}, Role: {}",
                    order.getMemberId(), memberId, role);
            throw new BadRequestException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        log.info("주문 상태 조회 완료 - Order ID: {}, Status: {}", orderId, order.getOrderStatus());

        return OrderApprovalStatusDTO.builder()
                .orderId(orderId)
                .orderNumber(order.getOrderNumber())
                .status(order.getOrderStatus())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    // WebSocket 기반 주문 승인 요청
    public void requestOrderApprovalWebSocket(Long orderId, Role role, Long userId) {
        log.info("=== WebSocket 주문 승인 요청 시작 === Order ID: {}, Role: {}, User ID: {}", orderId, role, userId);

        // 권한 체크 (ADMIN 또는 SUPER_ADMIN만 가능)
        if (role != Role.ADMIN && role != Role.SUPER_ADMIN) {
            log.error("권한 부족 - Order ID: {}, Role: {}", orderId, role);
            throw new UnauthorizedException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        try {
            // 주문 존재 및 상태 확인
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));

            if (order.getOrderStatus() != OrderStatus.ORDER_COMPLETED) {
                throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS_FOR_APPROVAL.getMessage());
            }

            // 승인 시도 ID 생성
            String approvalAttemptId = "WS-" + System.currentTimeMillis() + "-" + orderId;
            log.info("승인 시도 ID 생성 - Order ID: {}, Attempt ID: {}", orderId, approvalAttemptId);

            // 주문 상태를 PENDING_APPROVAL로 변경
            orderTransactionService.updateOrderStatusToApproval(orderId, approvalAttemptId);

            // WebSocket으로 상태 업데이트 전송 (요청자에게만)
            orderWebSocketHandler.sendToUser(
                    userId, 
                    orderId, 
                    OrderStatus.PENDING_APPROVAL, 
                    "STOCK_DEDUCTION", 
                    "재고 차감을 요청합니다.", 
                    null
            );

            // 5. OrderItems와 함께 로드 (LazyInitializationException 방지)
            Order orderWithItems = orderRepository.findByIdWithItems(orderId)
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));

            // 6. 재고 차감 요청 이벤트 생성
            List<StockDeductionRequestEvent.StockDeductionItem> items = orderWithItems.getOrderItems().stream()
                    .map(item -> StockDeductionRequestEvent.StockDeductionItem.builder()
                            .partId(item.getPartId())
                            .amount(item.getAmount())
                            .build())
                    .collect(Collectors.toList());

            StockDeductionRequestEvent event = StockDeductionRequestEvent.builder()
                    .orderId(orderId)
                    .approvalAttemptId(approvalAttemptId)
                    .items(items)
                    .build();

            // 7. Kafka 이벤트 발행
            try {
                kafkaProducerService.sendStockDeductionRequest(event);
                log.info("재고 차감 요청 이벤트 발행 완료 - Order ID: {}, Attempt ID: {}", orderId, approvalAttemptId);
            } catch (Exception e) {
                log.error("Kafka 이벤트 발행 실패 - Order ID: {}, 에러: {}", orderId, e.getMessage(), e);
                
                // Kafka 발행 실패 시 롤백
                orderTransactionService.rollbackOrderToCompleted(orderId);
                
                // WebSocket으로 실패 알림
                orderWebSocketHandler.sendOrderStatusUpdate(
                        orderId, 
                        OrderStatus.ORDER_COMPLETED, 
                        "ERROR", 
                        "주문 승인 처리 중 오류가 발생했습니다.", 
                        null
                );
            }

        } catch (Exception e) {
            log.error("WebSocket 주문 승인 처리 중 오류 발생 - Order ID: {}, 에러: {}", orderId, e.getMessage(), e);
            
            // WebSocket으로 에러 알림
            orderWebSocketHandler.sendOrderStatusUpdate(
                    orderId, 
                    OrderStatus.REJECTED, 
                    "ERROR", 
                    "주문 승인 처리 중 오류가 발생했습니다: " + e.getMessage(), 
                    null
            );
        }
    }

    // WebSocket 기반 재고 차감 성공 처리
    @Transactional
    public void handleStockDeductionSuccessWebSocket(StockDeductionSuccessEvent event) {
        log.info("=== WebSocket 재고 차감 성공 처리 시작 === Order ID: {}, Attempt ID: {}", 
                event.getOrderId(), event.getApprovalAttemptId());

        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));

            // 상태 및 Attempt ID 검증
            if (order.getOrderStatus() != OrderStatus.PENDING_APPROVAL) {
                log.warn("주문 상태가 PENDING_APPROVAL이 아님 - Order ID: {}, Current Status: {}", event.getOrderId(), order.getOrderStatus());
                return;
            }

            if (!event.getApprovalAttemptId().equals(order.getApprovalAttemptId())) {
                log.warn("승인 시도 ID가 일치하지 않음 - Order ID: {}, Event Attempt ID: {}, Order Attempt ID: {}", event.getOrderId(), event.getApprovalAttemptId(), order.getApprovalAttemptId());
                return;
            }

            // 주문 승인 처리
            order.approve();
            orderRepository.save(order);

            log.info("=== WebSocket 주문 승인 완료 === Order ID: {}, Status: {}", 
                    event.getOrderId(), order.getOrderStatus());

            // WebSocket으로 성공 알림
            orderWebSocketHandler.sendOrderStatusUpdate(
                    event.getOrderId(), 
                    OrderStatus.PENDING_SHIPPING, 
                    "COMPLETED", 
                    "주문 승인이 완료되었습니다.", 
                    null
            );

        } catch (Exception e) {
            log.error("WebSocket 재고 차감 성공 처리 중 오류 발생 - Order ID: {}, 에러: {}", event.getOrderId(), e.getMessage(), e);
        }
    }

    // WebSocket 기반 재고 차감 실패 처리
    @Transactional
    public void handleStockDeductionFailedWebSocket(StockDeductionFailedEvent event) {
        log.info("=== WebSocket 재고 차감 실패 처리 시작 === Order ID: {}, Attempt ID: {}", 
                event.getOrderId(), event.getApprovalAttemptId());

        try {
            Order order = orderRepository.findById(event.getOrderId()).orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));

            // 상태 및 Attempt ID 검증
            if (order.getOrderStatus() != OrderStatus.PENDING_APPROVAL) {
                log.warn("주문 상태가 PENDING_APPROVAL이 아님 - Order ID: {}, Current Status: {}", event.getOrderId(), order.getOrderStatus());
                return;
            }

            if (!event.getApprovalAttemptId().equals(order.getApprovalAttemptId())) {
                log.warn("승인 시도 ID가 일치하지 않음 - Order ID: {}, Event Attempt ID: {}, Order Attempt ID: {}", event.getOrderId(), event.getApprovalAttemptId(), order.getApprovalAttemptId());
                return;
            }

            // 주문을 ORDER_COMPLETED로 롤백
            order.rollbackToOrderCompleted();
            orderRepository.save(order);

            log.info("=== WebSocket 주문 승인 실패 처리 완료 === Order ID: {}, Status: {}", event.getOrderId(), order.getOrderStatus());

            // WebSocket으로 실패 알림
            orderWebSocketHandler.sendOrderStatusUpdate(
                    event.getOrderId(), 
                    OrderStatus.ORDER_COMPLETED, 
                    "FAILED", 
                    "재고 부족으로 주문 승인이 실패했습니다.", 
                    event.getData()
            );

        } catch (Exception e) {
            log.error("WebSocket 재고 차감 실패 처리 중 오류 발생 - Order ID: {}, 에러: {}", 
                    event.getOrderId(), e.getMessage(), e);
        }
    }

}