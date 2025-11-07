package com.stockmate.order.api.order.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Order Entity 테스트")
class OrderTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .orderId(1L)
                .orderNumber("SMO-1")
                .totalPrice(150000)
                .paymentType(PaymentType.CARD)
                .requestedShippingDate(LocalDate.now().plusDays(1))
                .orderStatus(OrderStatus.ORDER_COMPLETED)
                .memberId(1L)
                .etc("테스트 주문")
                .build();
    }

    @Test
    @DisplayName("주문 취소 테스트")
    void cancel() {
        // when
        order.cancel();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("주문 승인 시작 테스트")
    void startApproval() {
        // given
        String attemptId = "ATTEMPT-123";

        // when
        order.startApproval(attemptId);

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING_APPROVAL);
        assertThat(order.getApprovalAttemptId()).isEqualTo(attemptId);
        assertThat(order.getApprovalStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("출고 대기 상태 변경 테스트")
    void pendingShipping() {
        // when
        order.pendingShipping();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING_SHIPPING);
    }

    @Test
    @DisplayName("주문 승인 테스트")
    void approve() {
        // when
        order.approve();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.APPROVAL_ORDER);
    }

    @Test
    @DisplayName("주문 반려 테스트")
    void reject() {
        // given
        String reason = "재고 부족";

        // when
        order.reject(reason);

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getRejectedMessage()).isEqualTo(reason);
    }

    @Test
    @DisplayName("주문 승인 실패 시 롤백 테스트")
    void rollbackToPayCompleted() {
        // given
        order.startApproval("ATTEMPT-123");

        // when
        order.rollbackToPayCompleted();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAY_COMPLETED);
    }

    @Test
    @DisplayName("배송 등록 테스트")
    void registerShipping() {
        // given
        String carrier = "현대글로비스";
        String trackingNumber = "1234567890123";

        // when
        order.registerShipping(carrier, trackingNumber);

        // then
        assertThat(order.getCarrier()).isEqualTo(carrier);
        assertThat(order.getTrackingNumber()).isEqualTo(trackingNumber);
        assertThat(order.getShippingDate()).isEqualTo(LocalDate.now());
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SHIPPING);
    }

    @Test
    @DisplayName("입고 처리 시작 테스트")
    void startReceiving() {
        // given
        String attemptId = "RECEIVING-123";

        // when
        order.startReceiving(attemptId);

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING_RECEIVING);
        assertThat(order.getApprovalAttemptId()).isEqualTo(attemptId);
        assertThat(order.getApprovalStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("입고 완료 테스트")
    void completeReceiving() {
        // when
        order.completeReceiving();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.RECEIVED);
    }

    @Test
    @DisplayName("입고 실패 시 롤백 테스트")
    void rollbackToShipping() {
        // given
        order.startReceiving("RECEIVING-123");

        // when
        order.rollbackToShipping();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SHIPPING);
    }

    @Test
    @DisplayName("주문번호 생성 테스트 (@PrePersist)")
    void generateOrderNumber() {
        // given
        Order newOrder = Order.builder()
                .totalPrice(100000)
                .paymentType(PaymentType.CARD)
                .orderStatus(OrderStatus.ORDER_COMPLETED)
                .memberId(1L)
                .build();

        // when
        newOrder.generateOrderNumber();

        // then
        assertThat(newOrder.getOrderNumber()).isNotNull();
        assertThat(newOrder.getOrderNumber()).startsWith("TEMP-");
    }

    @Test
    @DisplayName("주문 상태 변경 시나리오 테스트")
    void orderStatusLifecycle() {
        // 1. 주문 생성
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ORDER_COMPLETED);

        // 2. 주문 승인 시작
        order.startApproval("ATTEMPT-1");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING_APPROVAL);

        // 3. 주문 승인 완료
        order.approve();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.APPROVAL_ORDER);

        // 4. 출고 대기
        order.pendingShipping();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING_SHIPPING);

        // 5. 배송 등록
        order.registerShipping("현대글로비스", "1234567890123");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SHIPPING);

        // 6. 입고 처리 시작
        order.startReceiving("RECEIVING-1");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING_RECEIVING);

        // 7. 입고 완료
        order.completeReceiving();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.RECEIVED);
    }
}

