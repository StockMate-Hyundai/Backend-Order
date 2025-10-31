package com.stockmate.order.api.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySalesResponseDTO {
    private List<CategorySale> categories; // 카테고리별 판매량

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategorySale {
        private String categoryName; // 카테고리명
        private long totalQuantity;  // 해당 카테고리 총 판매 수량
    }
}

