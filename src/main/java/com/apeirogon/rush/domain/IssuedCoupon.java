package com.apeirogon.rush.domain;

/**
 * 발급된 쿠폰
 */
public record IssuedCoupon(
        Long userId,
        Long couponId
) {

    public long getUserId() {
        return userId;
    }

    public long getCouponId() {
        return couponId;
    }
}
