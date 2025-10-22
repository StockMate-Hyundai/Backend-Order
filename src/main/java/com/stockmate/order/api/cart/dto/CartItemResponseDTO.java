package com.stockmate.order.api.cart.dto;

import com.stockmate.order.api.cart.entity.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponseDTO {
    private Long cartItemId;
    private Long partId;
    private int amount;

    public static CartItemResponseDTO from(CartItem cartItem) {
        return CartItemResponseDTO.builder()
                .cartItemId(cartItem.getCartItemId())
                .partId(cartItem.getPartId())
                .amount(cartItem.getAmount())
                .build();
    }
}

