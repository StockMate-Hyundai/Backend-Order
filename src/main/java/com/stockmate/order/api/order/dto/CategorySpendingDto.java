package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CategorySpendingDto {
    private String categoryName;
    private Long totalAmount;
}
