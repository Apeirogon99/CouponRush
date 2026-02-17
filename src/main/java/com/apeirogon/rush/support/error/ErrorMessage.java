package com.apeirogon.rush.support.error;

public class ErrorMessage {
    private final String code;
    private final String message;
    private final Object data;

    private ErrorMessage() {
        this.code = null;
        this.message = null;
        this.data = null;
    }

    public ErrorMessage(ErrorType errorType) {
        this.code = errorType.getCode().name();
        this.message = errorType.getMessage();
        this.data = null;
    }

    public ErrorMessage(ErrorType errorType, Object data) {
        this.code = errorType.getCode().name();
        this.message = errorType.getMessage();
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
