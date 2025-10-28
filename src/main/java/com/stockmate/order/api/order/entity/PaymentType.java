package com.stockmate.order.api.order.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentType {
    DEPOSIT("DEPOSIT"),
    CARD("CARD");

    private final String key;

    @JsonValue
    public String getKey() {
        return key;
    }

    @JsonCreator
    public static PaymentType from(String value) {
        for (PaymentType type : PaymentType.values()) {
            if (type.getKey().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PaymentType: " + value);
    }
}