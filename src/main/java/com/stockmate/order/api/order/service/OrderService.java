package com.stockmate.order.api.order.service;

import com.stockmate.order.api.order.dto.*;
import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderItem;
import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.order.repository.OrderRepository;
import com.stockmate.order.common.config.security.Role;
import com.stockmate.order.common.config.security.SecurityUser;
import com.stockmate.order.common.exception.BadRequestException;
import com.stockmate.order.common.exception.NotFoundException;
import com.stockmate.order.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

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
}
