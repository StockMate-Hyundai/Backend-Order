package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheckItemResponseDTO {
    private Long partId;
    private int requestedAmount;
    private int availableStock;
    private boolean canOrder;
    private String categoryName;
    private String name;
}