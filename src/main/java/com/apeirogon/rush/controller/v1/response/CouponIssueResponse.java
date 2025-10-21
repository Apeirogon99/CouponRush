package com.apeirogon.rush.controller.v1.response;

/**
 * 쿠폰 발급 Response
 */
public class CouponIssueResponse {
    private final boolean success;
    private final String message;

    public CouponIssueResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
