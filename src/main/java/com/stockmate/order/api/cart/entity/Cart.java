package com.stockmate.order.api.cart.entity;

import com.stockmate.order.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Cart extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId; // 한 계정당 하나의 장바구니

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    // 장바구니 아이템 추가 또는 수량 업데이트
    public void addOrUpdateItem(Long partId, int amount) {
        // 이미 존재하는 부품인지 확인
        CartItem existingItem = cartItems.stream()
                .filter(item -> item.getPartId().equals(partId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // 이미 있으면 수량 업데이트
            existingItem.updateAmount(amount);
        } else {
            // 없으면 새로 추가
            CartItem newItem = CartItem.builder()
                    .cart(this)
                    .partId(partId)
                    .amount(amount)
                    .build();
            cartItems.add(newItem);
        }
    }

    // 장바구니 아이템 삭제
    public void removeItem(Long partId) {
        cartItems.removeIf(item -> item.getPartId().equals(partId));
    }

    // 장바구니 전체 비우기
    public void clearItems() {
        cartItems.clear();
    }
}

