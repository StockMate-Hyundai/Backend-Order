package com.stockmate.order.api.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    ORDER_COMPLETED("ORDER_COMPLETED"), // 주문 완료
    PAYMENT_COMPLETED("PAYMENT_COMPLETED"), // 결제 완료
    SHIPPING("SHIPPING"), // 배송중
    REJECTED("REJECTED"), // 반려됨
    DELIVERED("DELIVERED"); // 배송 완료

    private final String key;
}
