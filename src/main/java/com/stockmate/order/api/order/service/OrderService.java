package com.stockmate.order.api.order.service;

import com.stockmate.order.api.order.dto.*;
import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderItem;
import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.order.entity.PaymentType;
import com.stockmate.order.api.order.repository.OrderRepository;
import com.stockmate.order.api.websocket.handler.OrderWebSocketHandler;
import com.stockmate.order.common.config.security.Role;
import com.stockmate.order.common.config.security.SecurityUser;
import com.stockmate.order.common.config.webClient.WebClientConfig;
import com.stockmate.order.common.exception.BadRequestException;
import com.stockmate.order.common.exception.InternalServerException;
import com.stockmate.order.common.exception.NotFoundException;
import com.stockmate.order.common.exception.UnauthorizedException;
import com.stockmate.order.common.producer.KafkaProducerService;
import com.stockmate.order.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final UserService userService;
    private final KafkaProducerService kafkaProducerService;
    private final OrderTransactionService orderTransactionService;
    private final OrderWebSocketHandler orderWebSocketHandler;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public MakeOrderResponseDto makeOrder(OrderRequestDTO orderRequestDTO, Long memberId) {
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

        PaymentType paymentType;
        try {
            paymentType = PaymentType.valueOf(String.valueOf(orderRequestDTO.getPaymentType()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("유효하지 않은 결제 방식입니다.");
        }

        Order order = Order.builder()
                .totalPrice(checkResult.getTotalPrice())
                .paymentType(paymentType)
                .requestedShippingDate(orderRequestDTO.getRequestedShippingDate())
                .shippingDate(null)
                .carrier(null)
                .trackingNumber(null)
                .rejectedMessage(null)
                .orderStatus(OrderStatus.ORDER_COMPLETED)
                .etc(orderRequestDTO.getEtc())
                .memberId(memberId) // 가맹점 ID
                .orderItems(new ArrayList<>())
                .build();


        // 재고검증 응답을 partId -> itemInfo 맵으로
        Map<Long, InventoryCheckItemResponseDTO> itemByPartId = Optional.ofNullable(checkResult.getOrderList())
                .orElseGet(Collections::emptyList)
                .stream()
                .collect(Collectors.toMap(InventoryCheckItemResponseDTO::getPartId, Function.identity(), (a, b) -> a));

        // 요청된 아이템을 주문 아이템으로 변환
        for (OrderItemRequestDTO itemRequest : orderRequestDTO.getOrderItems()) {
            InventoryCheckItemResponseDTO info = itemByPartId.get(itemRequest.getPartId());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .partId(itemRequest.getPartId())
                    .amount(itemRequest.getAmount())
                    .categoryName(info.getCategoryName())
                    .name(info.getName())
                    .build();

            order.getOrderItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        String orderNumber = "SMO-" + savedOrder.getOrderId();
        savedOrder.setOrderNumber(orderNumber);

        PayRequestEvent payRequestEvent = PayRequestEvent.of(savedOrder, memberId);

        applicationEventPublisher.publishEvent(payRequestEvent);

        log.info("주문 완료 - Order ID: {}, Order Number: {}, Member ID: {}, 주문 항목 수: {}, 총 금액: {}, Status: {}",
                savedOrder.getOrderId(), savedOrder.getOrderNumber(), savedOrder.getMemberId(),
                savedOrder.getOrderItems().size(), checkResult.getTotalPrice(),
                savedOrder.getOrderStatus()
        );
        return MakeOrderResponseDto.of(savedOrder);
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

        CancelRequestEvent cancelRequestEvent = CancelRequestEvent.of(order, memberId);

        try {
            kafkaProducerService.sendCancelRequest(cancelRequestEvent);
            log.info("결제 취소 요청 이벤트 발송 완료 - Order ID: {}, 금액: {}",
                    order.getOrderId(), order.getTotalPrice());
        } catch (Exception e) {
            log.error("결제 취소 요청 이벤트 발송 실패 - Order ID: {}", order.getOrderId(), e);
            Order failedOrder = order.toBuilder().orderStatus(OrderStatus.FAILED).build();
            orderRepository.save(failedOrder);
            throw new BadRequestException("결제 취소 요청 처리 중 오류가 발생했습니다.");
        }

        order.cancel();
        orderRepository.save(order);

        log.info("주문 취소 완료 - Order ID: {}, Order Number: {}, 취소자 Role: {}",
                orderId, order.getOrderNumber(), role);
    }

    // 결제 성공 or 실패 이벤트 처리
    @Transactional
    public void changeOrderStatus(Long orderId, String orderStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", orderId);
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());
                });

        try {
            OrderStatus newStatus = OrderStatus.valueOf(orderStatus); // 문자열 → Enum 변환
            order.setOrderStatus(newStatus); // setter 호출
            log.info("✅ 주문 상태 변경 완료 - Order ID: {}, 상태: {}", orderId, newStatus);
        } catch (IllegalArgumentException e) {
            log.error("❌ 잘못된 주문 상태 입력: {}", orderStatus);
            throw new BadRequestException("유효하지 않은 주문 상태 값입니다: " + orderStatus);
        }
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

    // 주문 정보 검증 조회
    public OrderValidateDTO getValidateOrder(Long orderId, Long memberId) {
        log.info("주문 검증 조회 - Order ID: {}, Member ID: {}", orderId, memberId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", orderId);
                    return new NotFoundException("주문 정보를 찾을 수 없습니다.");
                });

        if (memberId != order.getMemberId()) {
            log.error("권한 없음 - Order의 Member ID: {}, 요청자 Member ID: {}, Role: {}",
                    order.getMemberId(), memberId);
            throw new BadRequestException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        log.info("주문 검증 조회 완료 - Order ID: {}, Order Number: {}, Status: {}",
                order.getOrderId(), order.getOrderNumber(), order.getOrderStatus());

        return OrderValidateDTO.of(order);
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
                .paymentType(order.getPaymentType())
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

    // 배송 등록
    @Transactional
    public ShippingRegistrationResponseDTO registerShipping(ShippingRegistrationRequestDTO requestDTO, Role role) {
        log.info("배송 등록 요청 - Order Number: {}, 요청자 Role: {}", requestDTO.getOrderNumber(), role);

        // 권한 확인: WAREHOUSE만 배송 등록 가능
//        if (role != Role.WAREHOUSE) {
//            log.error("권한 부족 - Role: {}", role);
//            throw new UnauthorizedException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
//        }

        // 주문 조회
        Order order = orderRepository.findByOrderNumber(requestDTO.getOrderNumber())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));

        // 주문 상태 확인 (출고 대기 상태만 배송 등록 가능)
        if (order.getOrderStatus() != OrderStatus.PENDING_SHIPPING) {
            log.warn("배송 등록 불가능한 상태 - Order Number: {}, Status: {}", requestDTO.getOrderNumber(), order.getOrderStatus());
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS_FOR_SHIPPING.getMessage());
        }

        // 배송 정보 생성
        String carrier = "현대글로비스";
        String trackingNumber = generateTrackingNumber();

        // 배송 등록
        order.registerShipping(carrier, trackingNumber);
        orderRepository.save(order);

        log.info("배송 등록 완료 - Order Number: {}, Carrier: {}, Tracking Number: {}",
                requestDTO.getOrderNumber(), carrier, trackingNumber);

        return ShippingRegistrationResponseDTO.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .carrier(carrier)
                .trackingNumber(trackingNumber)
                .shippingDate(order.getShippingDate())
                .build();
    }

    // 운송장 번호 생성 (13자리 랜덤 숫자)
    private String generateTrackingNumber() {
        StringBuilder trackingNumber = new StringBuilder();
        for (int i = 0; i < 13; i++) {
            trackingNumber.append((int) (Math.random() * 10));
        }
        return trackingNumber.toString();
    }

    /**
     * 주문 상태를 PENDING_SHIPPING으로 변경 (창고 관리자 전용)
     */
    @Transactional
    public void updateOrderStatusToPendingShipping(Long orderId, Role role) {
        log.info("주문 상태를 출고 대기로 변경 요청 - Order ID: {}, 요청자 Role: {}", orderId, role);

        // 권한 확인: WAREHOUSE만 가능
//        if (role != Role.WAREHOUSE) {
//            log.error("권한 부족 - Role: {}", role);
//            throw new UnauthorizedException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
//        }

        // 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", orderId);
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());
                });

        // 주문 상태 확인 (승인 완료 상태만 출고 대기로 변경 가능)
        if (order.getOrderStatus() != OrderStatus.APPROVAL_ORDER) {
            log.warn("출고 대기로 변경 불가능한 상태 - Order ID: {}, Status: {}", orderId, order.getOrderStatus());
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS_FOR_SHIPPING.getMessage());
        }

        // 주문 상태를 PENDING_SHIPPING으로 변경
        order.pendingShipping();
        orderRepository.save(order);

        log.info("주문 상태를 출고 대기로 변경 완료 - Order ID: {}, Status: PENDING_SHIPPING", orderId);
    }

    // 입고 처리 요청 (WebSocket 기반)
    @Transactional
    public void requestReceivingProcess(ReceivingProcessRequestDTO requestDTO, Role role, Long userId) {
        log.info("입고 처리 요청 - Order Number: {}, 요청자 Role: {}, User ID: {}", requestDTO.getOrderNumber(), role, userId);

        // 권한 확인: 가맹점 사용자만 입고 처리 가능 (USER 역할)
        if (role != Role.USER) {
            log.error("권한 부족 - Role: {}", role);
            throw new UnauthorizedException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        // 주문 조회 (OrderItems와 함께)
        Order order = orderRepository.findByOrderNumberWithItems(requestDTO.getOrderNumber())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));

        // 주문의 가맹점(memberId)과 요청자의 가맹점 ID가 일치하는지 확인
        if (!order.getMemberId().equals(userId)) {
            log.error("가맹점 불일치 - Order Member ID: {}, 요청자 ID: {}", order.getMemberId(), userId);
            throw new UnauthorizedException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        // 주문 상태 확인 (배송 중 상태만 입고 처리 가능)
        if (order.getOrderStatus() != OrderStatus.SHIPPING) {
            log.warn("입고 처리 불가능한 상태 - Order Number: {}, Status: {}", requestDTO.getOrderNumber(), order.getOrderStatus());
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS_FOR_RECEIVING.getMessage());
        }

        // 입고 처리 시도 ID 생성
        String attemptId = "RECEIVING_" + System.currentTimeMillis() + "_" + order.getOrderId();

        try {
            // 주문 상태를 입고 대기로 변경
            orderTransactionService.updateOrderStatusToReceiving(order.getOrderId(), attemptId);

            // 입고 처리 요청 이벤트 생성
            List<ReceivingItemDTO> items = order.getOrderItems().stream()
                    .map(item -> ReceivingItemDTO.builder()
                            .partId(item.getPartId())
                            .quantity(item.getAmount())
                            .build())
                    .toList();

            ReceivingProcessRequestEvent event = ReceivingProcessRequestEvent.builder()
                    .orderId(order.getOrderId())
                    .orderNumber(order.getOrderNumber())
                    .approvalAttemptId(attemptId)
                    .memberId(order.getMemberId()) // 가맹점 ID
                    .items(items)
                    .build();

            // Parts 서버로 직접 API 호출하여 재고 업데이트 (InventoryService 사용)
            try {
                // DTO를 Map으로 변환
                List<Map<String, Object>> itemList = items.stream()
                        .map(item -> {
                            Map<String, Object> itemMap = new HashMap<>();
                            itemMap.put("partId", item.getPartId());
                            itemMap.put("quantity", item.getQuantity());
                            return itemMap;
                        })
                        .collect(Collectors.toList());

                inventoryService.updateStoreInventory(order.getMemberId(), itemList);
                log.info("Parts 서버 재고 업데이트 완료 - Order ID: {}, Attempt ID: {}", order.getOrderId(), attemptId);

                // 재고 업데이트 성공 시 주문 상태를 RECEIVED로 변경
                order.completeReceiving();
                orderRepository.save(order);

                // Information 서버로 입고 히스토리 등록 API 호출
                String message = String.format("%s 주문 입고처리 되었습니다.", order.getOrderNumber());

                // 부품 상세 정보를 items 리스트로 변환
                // itemList에는 이미 partId와 quantity가 포함되어 있음
                inventoryService.registerReceivingHistory(
                        order.getMemberId(), // 가맹점 ID
                        order.getOrderId(), // 주문 ID
                        order.getOrderNumber(), // 주문 번호
                        message, // 메시지
                        "RECEIVED", // 상태
                        itemList // 부품 상세 정보 (partId, quantity 포함)
                );
                log.info("입고 히스토리 등록 완료 - Order Number: {}, 가맹점 ID: {}, 부품 종류: {}",
                        order.getOrderNumber(), order.getMemberId(), itemList.size());

                // WebSocket으로 상태 업데이트 전송 (요청자에게만)
                orderWebSocketHandler.sendToUser(
                        userId,
                        order.getOrderId(),
                        OrderStatus.RECEIVED, // 성공 시 RECEIVED 상태로 전송
                        "RECEIVING_PROCESS_SUCCESS",
                        "입고 처리가 완료되었습니다.",
                        null
                );

                log.info("입고 처리 완료 - Order ID: {}, Status: RECEIVED", order.getOrderId());

            } catch (Exception partsException) {
                log.error("❌ Parts 서버 재고 업데이트 실패 - Order ID: {}, 에러: {}", order.getOrderId(), partsException.getMessage(), partsException);

                // 실패 시 롤백
                orderTransactionService.rollbackOrderToShipping(order.getOrderId());
                log.info("주문 상태 롤백 완료 - Order ID: {}, Status: SHIPPING", order.getOrderId());

                // WebSocket으로 실패 알림 (요청자에게만)
                orderWebSocketHandler.sendToUser(
                        userId,
                        order.getOrderId(),
                        OrderStatus.SHIPPING, // 롤백된 상태
                        "RECEIVING_PROCESS_ERROR",
                        "재고 업데이트 중 오류가 발생했습니다: " + partsException.getMessage(),
                        null
                );

                throw new InternalServerException("재고 업데이트 실패: " + partsException.getMessage());
            }

        } catch (Exception e) {
            log.error("Kafka 이벤트 발행 실패 - Order ID: {}, 에러: {}", order.getOrderId(), e.getMessage(), e);

            // Kafka 발행 실패 시 롤백
            orderTransactionService.rollbackOrderToShipping(order.getOrderId());

            // WebSocket으로 실패 알림
            orderWebSocketHandler.sendOrderStatusUpdate(
                    order.getOrderId(),
                    OrderStatus.SHIPPING,
                    "ERROR",
                    "입고 처리 요청 중 오류가 발생했습니다.",
                    null
            );

            throw new InternalServerException(ErrorStatus.KAFKA_EVENT_EXCEPTION.getMessage());
        }
    }

    // 입고 처리 성공 처리 (WebSocket 기반)
    @Transactional
    public void handleReceivingProcessSuccessWebSocket(ReceivingProcessSuccessEvent event) {
        log.info("=== WebSocket 입고 처리 성공 처리 시작 === Order ID: {}, Attempt ID: {}",
                event.getOrderId(), event.getApprovalAttemptId());

        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));

            // 상태 및 시도 ID 검증
            if (order.getOrderStatus() != OrderStatus.PENDING_RECEIVING ||
                    !event.getApprovalAttemptId().equals(order.getApprovalAttemptId())) {
                log.warn("입고 처리 성공 이벤트 무시 - Order ID: {}, 현재 상태: {}, 현재 시도 ID: {}, 이벤트 시도 ID: {}",
                        event.getOrderId(), order.getOrderStatus(), order.getApprovalAttemptId(), event.getApprovalAttemptId());
                return;
            }

            // 입고 완료 처리
            order.completeReceiving();
            orderRepository.save(order);

            log.info("=== WebSocket 입고 처리 완료 === Order ID: {}, Status: {}",
                    event.getOrderId(), order.getOrderStatus());

            // WebSocket으로 성공 알림
            orderWebSocketHandler.sendOrderStatusUpdate(
                    event.getOrderId(),
                    OrderStatus.RECEIVED,
                    "COMPLETED",
                    "입고 처리가 완료되었습니다.",
                    null
            );

        } catch (Exception e) {
            log.error("WebSocket 입고 처리 성공 처리 중 오류 발생 - Order ID: {}, 에러: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }

    // 입고 처리 실패 처리 (WebSocket 기반)
    @Transactional
    public void handleReceivingProcessFailedWebSocket(ReceivingProcessFailedEvent event) {
        log.info("=== WebSocket 입고 처리 실패 처리 시작 === Order ID: {}, Attempt ID: {}",
                event.getOrderId(), event.getApprovalAttemptId());

        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));

            // 상태 및 시도 ID 검증
            if (order.getOrderStatus() != OrderStatus.PENDING_RECEIVING ||
                    !event.getApprovalAttemptId().equals(order.getApprovalAttemptId())) {
                log.warn("입고 처리 실패 이벤트 무시 - Order ID: {}, 현재 상태: {}, 현재 시도 ID: {}, 이벤트 시도 ID: {}",
                        event.getOrderId(), order.getOrderStatus(), order.getApprovalAttemptId(), event.getApprovalAttemptId());
                return;
            }

            // 배송 중 상태로 롤백
            order.rollbackToShipping();
            orderRepository.save(order);

            log.info("=== WebSocket 입고 처리 실패 롤백 완료 === Order ID: {}, Status: {}",
                    event.getOrderId(), order.getOrderStatus());

            // WebSocket으로 실패 알림
            orderWebSocketHandler.sendOrderStatusUpdate(
                    event.getOrderId(),
                    OrderStatus.SHIPPING,
                    "FAILED",
                    "입고 처리에 실패했습니다: " + event.getErrorMessage(),
                    event.getData()
            );

        } catch (Exception e) {
            log.error("WebSocket 입고 처리 실패 처리 중 오류 발생 - Order ID: {}, 에러: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
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
                pageable,
                true  // FAILED 상태 제외
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

        // TODO: 상태 변경 적용하기
        if (order.getOrderStatus() != OrderStatus.PAY_COMPLETED) {
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

            if (order.getOrderStatus() != OrderStatus.PAY_COMPLETED) {
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

            // OrderItems와 함께 로드 (LazyInitializationException 방지)
            Order orderWithItems = orderRepository.findByIdWithItems(orderId)
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));

            // 재고 차감 요청 데이터 생성
            List<Map<String, Object>> items = orderWithItems.getOrderItems().stream()
                    .map(item -> {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("partId", item.getPartId());
                        itemMap.put("amount", item.getAmount());
                        return itemMap;
                    })
                    .collect(Collectors.toList());

            // Parts 서버 재고 차감 API 직접 호출
            try {
                inventoryService.deductStock(orderId, orderWithItems.getOrderNumber(), items);
                log.info("재고 차감 API 호출 성공 - Order ID: {}", orderId);

                // 주문 승인 완료 처리
                orderTransactionService.approveOrder(orderId);
                log.info("주문 승인 완료 - Order ID: {}", orderId);

                // WebSocket으로 성공 알림 (요청자에게만)
                orderWebSocketHandler.sendToUser(
                        userId,
                        orderId,
                        OrderStatus.APPROVAL_ORDER,
                        "APPROVAL_SUCCESS",
                        "주문이 승인되었습니다.",
                        null
                );

            } catch (Exception e) {
                log.error("재고 차감 API 호출 실패 - Order ID: {}, 에러: {}", orderId, e.getMessage(), e);

                // API 호출 실패 시 롤백
                orderTransactionService.rollbackOrderToCompleted(orderId);

                // WebSocket으로 실패 알림 (요청자에게만)
                orderWebSocketHandler.sendToUser(
                        userId,
                        orderId,
                        OrderStatus.PAY_COMPLETED,
                        "ERROR",
                        "재고 차감 실패: " + e.getMessage(),
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
                    "주문 승인 처리 중 예상치 못한 오류가 발생했습니다: " + e.getMessage(),
                    null
            );
        }
    }

}