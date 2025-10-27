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
public class ReceivingProcessRequestEvent {
    private Long orderId;
    private String orderNumber;
    private String approvalAttemptId;
    private Long memberId;
    private List<ReceivingItemDTO> items;
}
