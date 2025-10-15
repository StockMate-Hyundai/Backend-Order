package com.stockmate.order.api.order.entity;

import com.stockmate.order.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    private String etc; // 주문 추가 설명
    private String rejectedMessage; // 반려 메세지
    private String carrier; // 택배 회사 이름
    private String trackingNumber; // 운송장 번호

    private LocalDate requestedShippingDate; // 원하는 출고 일자
    private LocalDate shippingDate; // 실제 출고 일자

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

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

}