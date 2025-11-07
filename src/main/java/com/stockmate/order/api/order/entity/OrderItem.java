package com.stockmate.order.api.order.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore // 순환 참조 해결
    private Order order;

    @Column(name = "part_id", nullable = false)
    private Long partId;

    private int amount; // 주문수량
    private String categoryName;
    private String name;
    private Long price; // 판매가 (주문 시점)
    private Long cost;  // 원가 (주문 시점)
    private String location; // 창고 위치 (예: "A3-3")
    private Double weight;
}
