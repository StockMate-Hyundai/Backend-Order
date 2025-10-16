package com.stockmate.order.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)

public enum ErrorStatus {
    /**
     * 400 BAD_REQUEST
     */
    VALIDATION_REQUEST_MISSING_EXCEPTION(HttpStatus.BAD_REQUEST, "요청 값이 입력되지 않았습니다."),
    USER_ALREADY_EXISTS_EXCEPTION(HttpStatus.BAD_REQUEST,"이미 존재하는 사용자입니다."),
    INVALID_ROLE_EXCEPTION(HttpStatus.BAD_REQUEST,"해당 요청을 수행할 권한이 없습니다."),
    SOLD_OUT_PARTS_EXCEPTION(HttpStatus.BAD_REQUEST,"부품 재고가 부족합니다."),
    ALREADY_CANCELLED_ORDER_EXCEPTION(HttpStatus.BAD_REQUEST,"이미 취소된 주문입니다."),
    ALREADY_SHIPPED_OR_DELIVERED_ORDER_EXCEPTION(HttpStatus.BAD_REQUEST,"배송 중이거나 완료된 주문은 취소할 수 없습니다."),
    PARTS_SERVER_UNAVAILABLE_EXCEPTION(HttpStatus.SERVICE_UNAVAILABLE,"부품 서버가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요."),
    USER_SERVER_UNAVAILABLE_EXCEPTION(HttpStatus.SERVICE_UNAVAILABLE,"사용자 서버가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요."),

    /**
     * 401 UNAUTHORIZED
     */

    /**
     * 404 NOT_FOUND
     */
    USER_NOTFOUND_EXCEPTION(HttpStatus.NOT_FOUND,"해당 사용자를 찾을 수 없습니다."),
    ORDER_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND,"해당 주문을 찾을 수 없습니다."),

    /**
     * 500 SERVER_ERROR
     */
    KAFKA_EVENT_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR,"서버 내부 오류 발생"),
    NOT_CONNECTTION_PARTS_STOCK_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "부품 재고 체크 호출에 실패했습니다."),
    NOT_CONNECTTION_PARTS_DETAIL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "부품 상세 정보 조회에 실패했습니다."),
    NOT_CONNECTTION_USER_DETAIL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 정보 조회에 실패했습니다."),
    CHECK_PARTS_STOCK_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "부품 재고 체크 중 오류가 발생했습니다."),
    CHECK_USER_DETAIL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 정보 조회 중 오류가 발생했습니다."),
    CHECK_PARTS_DETAIL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "부품 상세 정보 조회 중 오류가 발생했습니다."),
    RESPONSE_DATA_NOT_MATCH_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR,"외부 서버 응답이 올바르지 않습니다."),
    RESPONSE_DATA_NULL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR,"외부 서버 응답 데이터가 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}