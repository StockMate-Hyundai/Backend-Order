package com.stockmate.order.api.order.entity;

import com.stockmate.order.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(name = "order_number", unique = true)
    private String orderNumber;

    private int totalPrice; // 총 주문 금액

    private PaymentType paymentType; // 결제 방식

    private LocalDate requestedShippingDate; // 원하는 출고 일자
    private LocalDate shippingDate; // 실제 출고 일자

    private String carrier; // 택배 회사 이름
    private String trackingNumber; // 운송장 번호

    private String rejectedMessage; // 반려 메세지

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 50)
    private OrderStatus orderStatus;

    private String etc; // 주문 추가 설명

    @Column(name = "approval_attempt_id")
    private String approvalAttemptId; // Saga 시도 식별자 (레이스 컨디션 방지)

    @Column(name = "approval_started_at")
    private LocalDateTime approvalStartedAt; // 승인 시작 시간 (장기 PENDING 방지용)

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @PrePersist
    public void generateOrderNumber() {
        if (this.orderNumber == null) {
            // 임시값으로 설정 (저장 후 orderId가 생성되면 실제 주문번호로 업데이트)
            this.orderNumber = "TEMP-" + System.currentTimeMillis();
        }
    }

    // 주문 취소
    public void cancel() {
        this.orderStatus = OrderStatus.CANCELLED;
    }

    // 주문 승인 요청 시작 (승인 대기 상태로 변경)
    public void startApproval(String attemptId) {
        this.orderStatus = OrderStatus.PENDING_APPROVAL;
        this.approvalAttemptId = attemptId;
        this.approvalStartedAt = LocalDateTime.now();
    }

    // 주문 승인 완료 (출고 대기 상태로 변경)
    public void approve() {
        this.orderStatus = OrderStatus.PENDING_SHIPPING;
    }

    // 주문 반려
    public void reject(String reason) {
        this.orderStatus = OrderStatus.REJECTED;
        this.rejectedMessage = reason;
    }

    // 주문 승인 실패 시 다시 주문 완료 상태로 되돌림
    public void rollbackToOrderCompleted() {
        this.orderStatus = OrderStatus.ORDER_COMPLETED;
    }

}