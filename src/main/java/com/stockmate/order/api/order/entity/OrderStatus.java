package com.stockmate.order.api.order.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    ORDER_COMPLETED("ORDER_COMPLETED"), // 주문 완료
    PENDING_APPROVAL("PENDING_APPROVAL"), // 승인 대기 (재고 차감 및 결제 진행 중)
    FAILED("FAILED"), // 결제 실패
    PENDING_SHIPPING("PENDING_SHIPPING"), // 출고 대기
    SHIPPING("SHIPPING"), // 배송중
    PENDING_RECEIVING("PENDING_RECEIVING"), // 입고 대기
    REJECTED("REJECTED"), // 출고 반려
    DELIVERED("DELIVERED"), // 배송 완료
    RECEIVED("RECEIVED"), // 입고 완료
    REFUNDED("REFUNDED"), // 환불 완료
    REFUND_REJECTED("REFUND_REJECTED"),
    CANCELLED("CANCELLED"); // 주문 취소

    private final String key;

    @JsonValue
    public String getKey() { return key; }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)  // ← 이 모드가 핵심
    public static OrderStatus from(String value) {
        for (OrderStatus o : values()) {
            if (o.key.equalsIgnoreCase(value)) return o;
        }
        throw new IllegalArgumentException("Unknown OrderStatus: " + value);
    }
}
