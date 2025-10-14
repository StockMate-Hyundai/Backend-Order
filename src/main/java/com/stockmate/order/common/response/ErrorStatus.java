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
    ALREADY_USER_STATUS_EXCEPTION(HttpStatus.BAD_REQUEST,"이미 처리된 유저 상태 입니다."),
    ALREADY_USER_ROLE_EXCEPTION(HttpStatus.BAD_REQUEST,"이미 처리된 유저 권한 입니다."),

    /**
     * 401 UNAUTHORIZED
     */
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"인증되지 않은 사용자입니다."),

    /**
     * 404 NOT_FOUND
     */
    USER_NOTFOUND_EXCEPTION(HttpStatus.NOT_FOUND,"해당 사용자를 찾을 수 없습니다."),

    /**
     * 500 SERVER_ERROR
     */
    GEOCODING_FAILED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR,"주소 지오코딩에 실패했습니다."),
    KAFKA_EVENT_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR,"서버 내부 오류 발생"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}