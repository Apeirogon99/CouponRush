package com.apeirogon.rush.messaging;

public record CouponIssueEvent(
        Long userId,
        Long couponId,
        String couponKey,
        String userCouponSetKey
) {
}
