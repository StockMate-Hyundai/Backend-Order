package com.stockmate.order.api.cart.dto;

import com.stockmate.order.api.cart.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponseDTO {
    private Long cartId;
    private Long memberId;
    private List<CartItemResponseDTO> items;

    public static CartResponseDTO from(Cart cart) {
        return CartResponseDTO.builder()
                .cartId(cart.getCartId())
                .memberId(cart.getMemberId())
                .items(cart.getCartItems().stream()
                        .map(CartItemResponseDTO::from)
                        .collect(Collectors.toList()))
                .build();
    }
}

