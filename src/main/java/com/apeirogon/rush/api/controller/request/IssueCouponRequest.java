package com.apeirogon.rush.api.controller.request;

public record IssueCouponRequest(
        Long userId,
        Long couponId
) {
}
