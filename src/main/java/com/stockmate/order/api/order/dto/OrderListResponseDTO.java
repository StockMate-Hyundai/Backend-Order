package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderListResponseDTO {
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private boolean isLast;
    private List<OrderDetailResponseDTO> content;
}
