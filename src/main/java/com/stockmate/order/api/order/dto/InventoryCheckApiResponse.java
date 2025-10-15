package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckApiResponse {
    private int status;
    private boolean success;
    private String message;
    private InventoryCheckResponseDTO data;
}