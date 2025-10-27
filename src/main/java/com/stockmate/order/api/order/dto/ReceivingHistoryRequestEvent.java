package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceivingHistoryRequestEvent {
    private Long orderId; // 주문 ID
    private String approvalAttemptId; // 승인 시도 ID
    private Long memberId; // 가맹점 ID
    private String orderNumber; // 주문 번호
    private String message; // 메시지 (예: 'SMO-22 주문 입고처리 되었습니다.')
    private String status; // 상태 (예: 'RECEIVED')
}