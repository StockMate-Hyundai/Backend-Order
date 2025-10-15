package com.stockmate.order.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseException{
    public BadRequestException() {
        super(HttpStatus.BAD_REQUEST);
    }

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public BadRequestException(String message, Object data) {
        super(HttpStatus.BAD_REQUEST, message, data);
    }
}