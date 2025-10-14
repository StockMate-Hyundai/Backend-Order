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

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "part_id", nullable = false)
    private Long partId;

    private int amount; // 주문수량
    private String etc; // 주문 추가 설명
    private String rejectedMessage; // 반려 메세지
    private String carrier; // 택배 회사 이름
    private String trackingNumber; // 운송장 번호

    private LocalDate requestedShippingDate; // 원하는 출고 일자
    private LocalDate shippingDate; // 실제 출고 일자

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

}
