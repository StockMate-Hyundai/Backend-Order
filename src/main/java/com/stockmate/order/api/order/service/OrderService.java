package com.stockmate.order.api.order.service;

import com.stockmate.order.api.order.dto.*;
import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderItem;
import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.order.repository.OrderRepository;
import com.stockmate.order.common.config.security.Role;
import com.stockmate.order.common.config.security.SecurityUser;
import com.stockmate.order.common.exception.BadRequestException;
import com.stockmate.order.common.exception.BaseException;
import com.stockmate.order.common.exception.NotFoundException;
import com.stockmate.order.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.async.DeferredResult;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final UserService userService;
    private final com.stockmate.order.common.producer.KafkaProducerService kafkaProducerService;
    private final OrderTransactionService orderTransactionService; // Self-Invocation 해결용

    // 주문 승인 대기 중인 DeferredResult를 관리하는 맵 (Order ID -> DeferredResult)
    private final ConcurrentHashMap<Long, DeferredResult<OrderStatus>> pendingApprovals = new ConcurrentHashMap<>();
    private static final long APPROVAL_TIMEOUT_MILLIS = 10000L; // 10초 타임아웃

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

        // 부품 재고 체크
        InventoryCheckResponseDTO checkResult = inventoryService.checkInventory(checkItems);

        log.info("재고 체크 완료 - 총 금액: {}", checkResult.getTotalPrice());

        // 주문 생성
        Order order = Order.builder()
                .memberId(memberId)
                .etc(orderRequestDTO.getEtc())
                .requestedShippingDate(orderRequestDTO.getRequestedShippingDate())
                .totalPrice(checkResult.getTotalPrice())
                .orderStatus(OrderStatus.ORDER_COMPLETED)
                .rejectedMessage(null)
                .carrier(null)
                .trackingNumber(null)
                .shippingDate(null)
                .orderItems(new ArrayList<>())
                .build();

        // 주문 항목들 생성 및 추가
        for (OrderItemRequestDTO itemRequest : orderRequestDTO.getOrderItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .partId(itemRequest.getPartId())
                    .amount(itemRequest.getAmount())
                    .build();

            order.getOrderItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // 주문번호 생성 및 업데이트 (TEMP-xxx -> SMO-xxx)
        String orderNumber = "SMO-" + savedOrder.getOrderId();
        Order updatedOrder = savedOrder.toBuilder()
                .orderNumber(orderNumber)
                .build();
        Order finalOrder = orderRepository.save(updatedOrder);

        log.info("부품 발주 완료 - Order ID: {}, Order Number: {}, Member ID: {}, 주문 항목 수: {}, 총 금액: {}, Status: {}", 
                finalOrder.getOrderId(), finalOrder.getOrderNumber(), finalOrder.getMemberId(), 
                finalOrder.getOrderItems().size(), checkResult.getTotalPrice(), 
                finalOrder.getOrderStatus());

        // TODO: 추후 결제 서버에 totalPrice 전송
        // paymentService.processPayment(checkResult.getTotalPrice(), savedOrder.getOrderId());
    }

    // 주문 취소
    @Transactional
    public void cancelOrder(Long orderId, Long memberId, Role role) {
        log.info("주문 취소 요청 - Order ID: {}, Member ID: {}, Role: {}", orderId, memberId, role);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", orderId);
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());
                });

        // 주문자 확인 (ADMIN, SUPER_ADMIN은 모든 주문 취소 가능)
        boolean isAdmin = role == Role.ADMIN || role == Role.SUPER_ADMIN;
        if (!isAdmin && !order.getMemberId().equals(memberId)) {
            log.error("권한 없음 - Order의 Member ID: {}, 요청자 Member ID: {}, Role: {}", 
                    order.getMemberId(), memberId, role);
            throw new BadRequestException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        // 이미 취소된 주문인지 확인
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            log.warn("이미 취소된 주문 - Order ID: {}", orderId);
            throw new BadRequestException(ErrorStatus.ALREADY_CANCELLED_ORDER_EXCEPTION.getMessage());
        }

        // 배송 중이거나 완료된 주문은 취소 불가 (ADMIN, SUPER_ADMIN은 가능)
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

    // 주문 물리적 삭제 (테스트용 - ADMIN, SUPER_ADMIN만 가능)
    @Transactional
    public void deleteOrder(Long orderId, SecurityUser securityUser) {
        Role role = securityUser.getRole();
        Long adminId = securityUser.getMemberId();

        log.info("주문 물리적 삭제 요청 - Order ID: {}, 요청자 ID: {}, 요청자 Role: {}", orderId, adminId, role);

        // ADMIN, SUPER_ADMIN 권한 확인
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

    // 관리자용 주문 리스트 조회
    @Transactional(readOnly = true)
    public OrderListResponseDTO getOrderList(OrderListRequestDTO requestDTO, Role role) {
        log.info("주문 리스트 조회 - Status: {}, PartId: {}, MemberId: {}, StartDate: {}, EndDate: {}, Page: {}, Size: {}, Role: {}",
                requestDTO.getStatus(), requestDTO.getPartId(), requestDTO.getMemberId(),
                requestDTO.getStartDate(), requestDTO.getEndDate(), requestDTO.getPage(), requestDTO.getSize(), role);

        // 권한 체크 (ADMIN, SUPER_ADMIN만 가능)
        if (role != Role.ADMIN && role != Role.SUPER_ADMIN) {
            log.error("권한 부족 - Role: {}", role);
            throw new BadRequestException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        // 페이지 사이즈 검증
        int page = requestDTO.getPage() < 0 ? 0 : requestDTO.getPage();
        int size = (requestDTO.getSize() <= 0 || requestDTO.getSize() > 200) ? 20 : requestDTO.getSize();

        Pageable pageable = PageRequest.of(page, size);

        // 주문 조회 (QueryDSL 사용)
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

        // 모든 주문에서 중복 제거한 memberId 수집
        Set<Long> memberIds = orderPage.getContent().stream()
                .map(Order::getMemberId)
                .collect(Collectors.toSet());

        // 모든 주문에서 중복 제거한 partId 수집
        Set<Long> partIds = orderPage.getContent().stream()
                .flatMap(order -> order.getOrderItems().stream())
                .map(OrderItem::getPartId)
                .collect(Collectors.toSet());

        log.info("외부 서버 호출 준비 - 사용자 수: {}, 부품 수: {}", memberIds.size(), partIds.size());

        // 사용자 정보 일괄 조회
        Map<Long, UserBatchResponseDTO> userMap = userService.getUsersByMemberIds(new ArrayList<>(memberIds));

        // 부품 정보 일괄 조회
        Map<Long, PartDetailResponseDTO> partMap = inventoryService.getPartDetails(new ArrayList<>(partIds));

        log.info("외부 서버 호출 완료 - 조회된 사용자: {}, 조회된 부품: {}", userMap.size(), partMap.size());

        // 주문 데이터 조합
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

        // 주문 항목 상세 정보 구성
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

    // 주문 상세 조회
    @Transactional(readOnly = true)
    public OrderDetailResponseDTO getOrderDetail(Long orderId, Long memberId, Role role) {
        log.info("주문 상세 조회 - Order ID: {}, 요청자 Member ID: {}, Role: {}", orderId, memberId, role);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", orderId);
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());});

        // 권한 체크: 본인 주문 또는 ADMIN/SUPER_ADMIN
        boolean isAdmin = role == Role.ADMIN || role == Role.SUPER_ADMIN;
        if (!isAdmin && !order.getMemberId().equals(memberId)) {
            log.error("권한 없음 - Order의 Member ID: {}, 요청자 Member ID: {}, Role: {}", order.getMemberId(), memberId, role);
            throw new BadRequestException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        // 사용자 정보 조회
        List<Long> memberIds = List.of(order.getMemberId());
        Map<Long, UserBatchResponseDTO> userMap = userService.getUsersByMemberIds(memberIds);

        // 부품 정보 조회
        List<Long> partIds = order.getOrderItems().stream()
                .map(OrderItem::getPartId)
                .collect(Collectors.toList());
        Map<Long, PartDetailResponseDTO> partMap = inventoryService.getPartDetails(partIds);

        log.info("주문 상세 조회 완료 - Order ID: {}, Order Number: {}", orderId, order.getOrderNumber());

        return toOrderDetailResponseDTO(order, userMap, partMap);
    }

    // 사용자용 주문 리스트 조회 (내 주문만)
    @Transactional(readOnly = true)
    public OrderListResponseDTO getMyOrderList(MyOrderListRequestDTO requestDTO, Long memberId) {
        log.info("내 주문 리스트 조회 - Member ID: {}, Status: {}, StartDate: {}, EndDate: {}, Page: {}, Size: {}",
                memberId, requestDTO.getStatus(), requestDTO.getStartDate(), requestDTO.getEndDate(), 
                requestDTO.getPage(), requestDTO.getSize());

        // 페이지 사이즈 검증
        int page = requestDTO.getPage() < 0 ? 0 : requestDTO.getPage();
        int size = (requestDTO.getSize() <= 0 || requestDTO.getSize() > 200) ? 20 : requestDTO.getSize();

        Pageable pageable = PageRequest.of(page, size);

        // 주문 조회 (memberId 필터 강제 적용)
        Page<Order> orderPage = orderRepository.findOrdersWithFilters(
                requestDTO.getStatus(),
                null,  // partId 필터 없음
                memberId,  // 본인의 memberId만
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

        // 부품 ID 수집
        Set<Long> partIds = orderPage.getContent().stream()
                .flatMap(order -> order.getOrderItems().stream())
                .map(OrderItem::getPartId)
                .collect(Collectors.toSet());

        log.info("외부 서버 호출 준비 - 부품 수: {}", partIds.size());

        // 사용자 정보 조회 (본인 정보만)
        Map<Long, UserBatchResponseDTO> userMap = userService.getUsersByMemberIds(List.of(memberId));

        // 부품 정보 일괄 조회
        Map<Long, PartDetailResponseDTO> partMap = inventoryService.getPartDetails(new ArrayList<>(partIds));

        log.info("외부 서버 호출 완료 - 조회된 부품: {}", partMap.size());

        // 주문 데이터 조합
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

    // 주문 반려
    @Transactional
    public void requestOrderReject(OrderRejectRequestDTO orderRejectRequestDTO, Role role) {
        log.info("주문 반려 요청 - Order ID: {}, Role: {}", orderRejectRequestDTO.getOrderId(), role);

        // 권한 체크 (ADMIN, SUPER_ADMIN만 가능)
        if (role != Role.ADMIN && role != Role.SUPER_ADMIN) {
            log.error("권한 부족 - Role: {}", role);
            throw new BadRequestException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        Order order = orderRepository.findById(orderRejectRequestDTO.getOrderId())
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", orderRejectRequestDTO.getOrderId());
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());
                });

        // 주문 상태 확인 (ORDER_COMPLETED만 승인 가능)
        if (order.getOrderStatus() != OrderStatus.ORDER_COMPLETED) {
            log.warn("승인 불가능한 상태 - Order ID: {}, Status: {}", orderRejectRequestDTO.getOrderId(), order.getOrderStatus());
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS_FOR_APPROVAL.getMessage());
        }

        // 주문 상태를 REJECTED 변경
        order.reject(orderRejectRequestDTO.getReason());
        orderRepository.save(order);

        log.info("주문 반려 완료 - Order ID: {}, Order Number: {}, Status: REJECTED", orderRejectRequestDTO.getOrderId(), order.getOrderNumber());
    }

    // 주문 승인 요청 (비동기 응답 - DeferredResult 사용)
    public DeferredResult<OrderStatus> requestOrderApproval(Long orderId, Role role) {
        log.info("주문 승인 요청 (비동기) - Order ID: {}, Role: {}", orderId, role);

        // DeferredResult 생성 (10초 타임아웃)
        DeferredResult<OrderStatus> deferredResult = new DeferredResult<>(APPROVAL_TIMEOUT_MILLIS);

        // 타임아웃 콜백 설정
        deferredResult.onTimeout(() -> {
            log.warn("주문 승인 응답 타임아웃 - Order ID: {}, 백그라운드에서 계속 처리됨", orderId);
            pendingApprovals.remove(orderId);
            deferredResult.setResult(OrderStatus.PENDING_APPROVAL);
        });

        // 완료 콜백 설정 (정상 또는 타임아웃)
        deferredResult.onCompletion(() -> {
            log.debug("DeferredResult 완료 - Order ID: {}", orderId);
        });

        // 권한 체크 (ADMIN, SUPER_ADMIN만 가능)
        if (role != Role.ADMIN && role != Role.SUPER_ADMIN) {
            log.error("권한 부족 - Role: {}", role);
            deferredResult.setErrorResult(new BadRequestException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage()));
            return deferredResult;
        }

        // Saga 시도 식별자 생성
        String approvalAttemptId = UUID.randomUUID().toString();
        log.info("Saga 시도 식별자 생성 - Order ID: {}, Attempt ID: {}", orderId, approvalAttemptId);

        try {
            // 상태 변경을 별도 트랜잭션으로 먼저 커밋 (REQUIRES_NEW) - Self-Invocation 해결
            orderTransactionService.updateOrderStatusToApproval(orderId, approvalAttemptId);

        } catch (BadRequestException e) {
            // 상태 검증 실패 (이미 PENDING_APPROVAL 등) - 즉시 에러 응답
            log.error("주문 상태 변경 실패 - Order ID: {}, 에러: {}", orderId, e.getMessage());
            deferredResult.setErrorResult(e);
            return deferredResult;
        } catch (NotFoundException e) {
            // 주문을 찾을 수 없음 - 즉시 에러 응답
            log.error("주문을 찾을 수 없음 - Order ID: {}", orderId);
            deferredResult.setErrorResult(e);
            return deferredResult;
        }

        try {
            // DB 커밋 이후 OrderItems와 함께 로드 (LazyInitializationException 방지)
            Order order = orderRepository.findByIdWithItems(orderId)
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));

            // DeferredResult 등록
            pendingApprovals.put(orderId, deferredResult);

            // Kafka 이벤트 발행 (실패 시 보상 트랜잭션)
            List<StockDeductionRequestEvent.StockDeductionItem> items = order.getOrderItems().stream()
                    .map(item -> StockDeductionRequestEvent.StockDeductionItem.builder()
                            .partId(item.getPartId())
                            .amount(item.getAmount())
                            .build())
                    .collect(Collectors.toList());

            StockDeductionRequestEvent event = StockDeductionRequestEvent.builder()
                    .orderId(order.getOrderId())
                    .orderNumber(order.getOrderNumber())
                    .approvalAttemptId(approvalAttemptId)
                    .items(items)
                    .build();

            kafkaProducerService.sendStockDeductionRequest(event);

            log.info("주문 승인 요청 완료, 서블릿 스레드 해제 - Order ID: {}, Attempt ID: {}, Timeout: {}ms", 
                    orderId, approvalAttemptId, APPROVAL_TIMEOUT_MILLIS);

        } catch (Exception e) {
            // Kafka 발행 실패 또는 기타 에러 - 보상 트랜잭션 후 에러 응답
            log.error("Kafka 이벤트 발행 실패 또는 처리 중 에러 - Order ID: {}, 에러: {}", orderId, e.getMessage(), e);
            orderTransactionService.rollbackOrderToCompleted(orderId);
            pendingApprovals.remove(orderId);
            deferredResult.setErrorResult(e instanceof BaseException ? e : new BadRequestException("주문 승인 요청 중 오류가 발생했습니다."));
        }

        return deferredResult; // 서블릿 스레드 즉시 반환
    }

    // 주문 승인 상태 체크
    @Transactional(readOnly = true)
    public OrderStatus checkOrderApprovalStatus(Long orderId, Long memberId, Role role) {
        log.info("주문 승인 상태 체크 - Order ID: {}, Member ID: {}, Role: {}", orderId, memberId, role);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", orderId);
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());
                });

        // 권한 체크: 본인 주문 또는 ADMIN/SUPER_ADMIN
        boolean isAdmin = role == Role.ADMIN || role == Role.SUPER_ADMIN;
        if (!isAdmin && !order.getMemberId().equals(memberId)) {
            log.error("권한 없음 - Order의 Member ID: {}, 요청자 Member ID: {}, Role: {}", 
                    order.getMemberId(), memberId, role);
            throw new BadRequestException(ErrorStatus.INVALID_ROLE_EXCEPTION.getMessage());
        }

        log.info("주문 상태 조회 완료 - Order ID: {}, Status: {}", orderId, order.getOrderStatus());
        return order.getOrderStatus();
    }

    // 재고 차감 성공 이벤트 처리
    @Transactional
    public void handleStockDeductionSuccess(StockDeductionSuccessEvent event) {
        log.info("재고 차감 성공 처리 시작 - Order ID: {}, Attempt ID: {}", 
                event.getOrderId(), event.getApprovalAttemptId());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", event.getOrderId());
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());
                });

        // 상태가 PENDING_APPROVAL이고, attemptId가 일치하는지 확인 (레이스 컨디션 방지)
        if (order.getOrderStatus() != OrderStatus.PENDING_APPROVAL) {
            log.warn("주문 상태가 PENDING_APPROVAL이 아님 - Order ID: {}, Status: {}", 
                    event.getOrderId(), order.getOrderStatus());
            return;
        }

        if (!event.getApprovalAttemptId().equals(order.getApprovalAttemptId())) {
            log.warn("Attempt ID 불일치 (오래된 이벤트) - Order ID: {}, 이벤트 Attempt: {}, 주문 Attempt: {}", 
                    event.getOrderId(), event.getApprovalAttemptId(), order.getApprovalAttemptId());
            return;
        }

        // 주문 승인 완료 (출고 대기 상태로 변경)
        order.approve();
        orderRepository.save(order);

        log.info("재고 차감 성공 - 주문 승인 완료 - Order ID: {}, Status: PENDING_SHIPPING", event.getOrderId());

        // 트랜잭션 커밋 후 DeferredResult 완료 (TransactionSynchronization 사용)
        DeferredResult<OrderStatus> deferredResult = pendingApprovals.get(event.getOrderId());
        if (deferredResult != null && !deferredResult.isSetOrExpired()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 트랜잭션 커밋 후 DeferredResult 완료
                    pendingApprovals.remove(event.getOrderId());
                    deferredResult.setResult(OrderStatus.PENDING_SHIPPING);
                    log.info("트랜잭션 커밋 후 DeferredResult 완료 - Order ID: {}, Status: PENDING_SHIPPING", 
                            event.getOrderId());
                }
            });
        } else if (deferredResult != null) {
            log.info("DeferredResult 이미 완료됨 (타임아웃) - Order ID: {}", event.getOrderId());
            pendingApprovals.remove(event.getOrderId());
        }
    }

    // 재고 차감 실패 이벤트 처리 (보상 트랜잭션)
    @Transactional
    public void handleStockDeductionFailed(StockDeductionFailedEvent event) {
        log.info("재고 차감 실패 처리 시작 (보상 트랜잭션) - Order ID: {}, Attempt ID: {}", 
                event.getOrderId(), event.getApprovalAttemptId());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", event.getOrderId());
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());
                });

        // 상태가 PENDING_APPROVAL이고, attemptId가 일치하는지 확인 (레이스 컨디션 방지)
        if (order.getOrderStatus() != OrderStatus.PENDING_APPROVAL) {
            log.warn("주문 상태가 PENDING_APPROVAL이 아님 - Order ID: {}, Status: {}", 
                    event.getOrderId(), order.getOrderStatus());
            return;
        }

        if (!event.getApprovalAttemptId().equals(order.getApprovalAttemptId())) {
            log.warn("Attempt ID 불일치 (오래된 이벤트) - Order ID: {}, 이벤트 Attempt: {}, 주문 Attempt: {}", 
                    event.getOrderId(), event.getApprovalAttemptId(), order.getApprovalAttemptId());
            return;
        }

        // 주문을 다시 ORDER_COMPLETED로 되돌림 (보상 트랜잭션)
        order.rollbackToOrderCompleted();
        orderRepository.save(order);

        log.info("재고 차감 실패 - 주문 상태를 ORDER_COMPLETED로 되돌림 - Order ID: {}, Reason: {}", 
                event.getOrderId(), event.getReason());

        // 트랜잭션 커밋 후 DeferredResult 완료 (TransactionSynchronization 사용)
        DeferredResult<OrderStatus> deferredResult = pendingApprovals.get(event.getOrderId());
        if (deferredResult != null && !deferredResult.isSetOrExpired()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 트랜잭션 커밋 후 DeferredResult 완료
                    pendingApprovals.remove(event.getOrderId());
                    deferredResult.setResult(OrderStatus.ORDER_COMPLETED);
                    log.info("트랜잭션 커밋 후 DeferredResult 완료 - Order ID: {}, Status: ORDER_COMPLETED (실패 후 복원)", 
                            event.getOrderId());
                }
            });
        } else if (deferredResult != null) {
            log.info("DeferredResult 이미 완료됨 (타임아웃) - Order ID: {}", event.getOrderId());
            pendingApprovals.remove(event.getOrderId());
        }
    }

}
