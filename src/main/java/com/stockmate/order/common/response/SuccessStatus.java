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
    SEND_USER_INFO_SUCCESS(HttpStatus.OK,"사용자 정보 조회 성공"),
    SEND_HEALTH_CHECK_SUCCESS(HttpStatus.OK,"서버 상태 체크 성공"),
    SEND_USER_LIST_SUCCESS(HttpStatus.OK,"사용자 리스트 조회 성공"),
    SEND_USER_MODIFY_STATUS_SUCCESS(HttpStatus.OK,"사용자 상태 변경 성공"),
    SEND_USER_MODIFY_ROLE_SUCCESS(HttpStatus.OK,"사용자 역할 변경 성공"),

    /**
     * 201
     */
    SEND_USER_REGISTRATION_SUCCESS(HttpStatus.CREATED,"사용자 등록 성공"),

    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}