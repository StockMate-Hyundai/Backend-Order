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
public class UpdateCartRequestDTO {
    private List<CartItemRequestDTO> items; // 전체 장바구니 데이터
}

