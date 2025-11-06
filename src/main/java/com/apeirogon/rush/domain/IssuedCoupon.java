package com.apeirogon.rush.domain;

/**
 *  발급된 쿠폰
 */
public class IssuedCoupon {
    private long userId;
    private long couponId;

    public IssuedCoupon(long userId, long couponId) {
        this.userId = userId;
        this.couponId = couponId;
    }

    public long getUserId() {
        return userId;
    }

    public long getCouponId() {
        return couponId;
    }
}
