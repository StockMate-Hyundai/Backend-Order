package com.stockmate.order.api.cart.entity;

import com.stockmate.order.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CartItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "part_id", nullable = false)
    private Long partId; // 부품 ID

    @Column(name = "amount", nullable = false)
    private int amount; // 수량

    // 수량 업데이트
    public void updateAmount(int amount) {
        this.amount = amount;
    }
}

