package com.stockmate.order.api.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDetailResponseDTO {
    private Long cartItemId;
    private Long partId;
    private int amount;
    
    // 부품 상세 정보
    private String partName;
    private String categoryName;
    private String brand;
    private String model;
    private String trim;
    private int price;
    private int stock;
}

