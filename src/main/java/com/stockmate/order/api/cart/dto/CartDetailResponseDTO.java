package com.stockmate.order.api.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDetailResponseDTO {
    private Long cartId;
    private Long memberId;
    private List<CartItemDetailResponseDTO> items;
    private int totalPrice; // 총 금액 (모든 아이템의 price * amount 합)
}

