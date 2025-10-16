package com.stockmate.order.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum SuccessStatus {

    /**
     * 200
     */
    SEND_HEALTH_CHECK_SUCCESS(HttpStatus.OK,"서버 상태 체크 성공"),
    SEND_CANCELLED_ORDER_SUCCESS(HttpStatus.OK, "주문 취소 성공"),
    DELETE_ORDER_SUCCESS(HttpStatus.OK, "주문 삭제 성공"),

    /**
     * 201
     */
    SEND_PARTS_ORDER_SUCCESS(HttpStatus.CREATED,"부품 주문 성공"),

    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}