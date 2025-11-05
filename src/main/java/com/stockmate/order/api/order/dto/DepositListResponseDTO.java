package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class DepositListResponseDTO {
    private String orderId;
    private List<DepositPartDetailDTO> orderItems;
}
