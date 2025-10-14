package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckResponseDTO {
    private Long partId;
    private int stock;
    private boolean canOrder;
}
