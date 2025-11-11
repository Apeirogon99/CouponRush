package com.apeirogon.rush.support.error;

import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

public enum ErrorType {

    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "An unexpected error has occurred.", LogLevel.ERROR),
    COUPON_FAIL_REGISTER(HttpStatus.BAD_REQUEST, ErrorCode.E400, "Coupon fail register.", LogLevel.ERROR),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.E404, "Coupon not found.", LogLevel.ERROR),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, ErrorCode.E409, "Coupon already issued.", LogLevel.ERROR),
    COUPON_SOLD_OUT(HttpStatus.CONFLICT, ErrorCode.E409, "Coupon sold out.", LogLevel.ERROR),
    COUPON_ENDED(HttpStatus.GONE, ErrorCode.E410, "Coupon ended.", LogLevel.ERROR);

    private final HttpStatus status;

    private final ErrorCode code;

    private final String message;

    private final LogLevel logLevel;

    ErrorType(HttpStatus status, ErrorCode code, String message, LogLevel logLevel) {

        this.status = status;
        this.code = code;
        this.message = message;
        this.logLevel = logLevel;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

}
