package com.stockmate.order.api.order.service;

import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.order.repository.OrderRepository;
import com.stockmate.order.common.exception.BadRequestException;
import com.stockmate.order.common.exception.NotFoundException;
import com.stockmate.order.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTransactionService {

    private final OrderRepository orderRepository;

    /**
     * 주문 상태를 PENDING_APPROVAL로 변경 (별도 트랜잭션 - REQUIRES_NEW)
     * Self-Invocation 문제를 피하기 위해 별도 서비스로 분리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateOrderStatusToApproval(Long orderId, String approvalAttemptId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음 - Order ID: {}", orderId);
                    return new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage());
                });

        // 주문 상태 확인 (ORDER_COMPLETED만 승인 가능)
        if (order.getOrderStatus() != OrderStatus.ORDER_COMPLETED) {
            log.warn("승인 불가능한 상태 - Order ID: {}, Status: {}", orderId, order.getOrderStatus());
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS_FOR_APPROVAL.getMessage());
        }

        // 주문 상태를 PENDING_APPROVAL로 변경
        order.startApproval(approvalAttemptId);
        orderRepository.save(order);

        log.info("주문 상태 변경 완료 (별도 트랜잭션) - Order ID: {}, Status: PENDING_APPROVAL, Attempt ID: {}", 
                orderId, approvalAttemptId);
    }

    /**
     * 주문 승인 완료 (별도 트랜잭션 - REQUIRES_NEW)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void approveOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));
        
        if (order.getOrderStatus() != OrderStatus.PENDING_APPROVAL) {
            log.warn("승인 불가능한 상태 - Order ID: {}, Status: {}", orderId, order.getOrderStatus());
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS_FOR_APPROVAL.getMessage());
        }

        order.approve();
        orderRepository.save(order);
        log.info("주문 승인 완료 - Order ID: {}, Status: PENDING_SHIPPING", orderId);
    }

    /**
     * 재고 차감 실패 시 주문 상태를 ORDER_COMPLETED로 복원 (별도 트랜잭션 - REQUIRES_NEW)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackOrderToCompleted(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));
        
        if (order.getOrderStatus() == OrderStatus.PENDING_APPROVAL) {
            order.rollbackToOrderCompleted();
            orderRepository.save(order);
            log.info("재고 차감 실패로 주문 상태 복원 - Order ID: {}, Status: ORDER_COMPLETED", orderId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateOrderStatusToReceiving(Long orderId, String attemptId) {
        log.info("주문 상태를 입고 대기로 변경 시작 - Order ID: {}, Attempt ID: {}", orderId, attemptId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));

        if (order.getOrderStatus() != OrderStatus.SHIPPING) {
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS_FOR_RECEIVING.getMessage());
        }

        order.startReceiving(attemptId);
        orderRepository.save(order);

        log.info("주문 상태를 입고 대기로 변경 완료 - Order ID: {}, Status: {}", orderId, order.getOrderStatus());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackOrderToShipping(Long orderId) {
        log.info("주문 상태를 배송 중으로 롤백 시작 - Order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND_EXCEPTION.getMessage()));

        order.rollbackToShipping();
        orderRepository.save(order);

        log.info("주문 상태를 배송 중으로 롤백 완료 - Order ID: {}, Status: {}", orderId, order.getOrderStatus());
    }
}

