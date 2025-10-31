package com.stockmate.order.api.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentOrdersResponseDTO {
    private List<OrderInfo> orders; // 최근 주문 목록

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderInfo {
        private LocalDateTime createdAt;    // 주문 시간
        private String orderNumber;        // 주문 번호
        private int totalItemQuantity;     // 부품 주문 수량 (총합)
        private int totalPrice;            // 총 가격
        private String userName;           // 사용자 이름 (storeName 또는 owner)
    }
}

