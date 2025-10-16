package com.stockmate.order.api.order.dto;

import com.stockmate.order.api.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderListRequestDTO {
    private OrderStatus status;
    private Long partId;
    private Long memberId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int page;
    private int size;
}
