package com.stockmate.order.api.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentType {
    DEPOSIT("DEPOSIT"), // 예치금
    CARD("CARD"); // 카드

    private final String key;
}
