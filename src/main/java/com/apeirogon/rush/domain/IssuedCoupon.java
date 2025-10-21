package com.apeirogon.rush.domain;

/**
 *  발급된 쿠폰
 */
public class IssuedCoupon {
    private long userId;
    private long couponId;
    private String code;

    public IssuedCoupon(long userId, long couponId, String code) {
        this.userId = userId;
        this.couponId = couponId;
        this.code = code;
    }

    public long getUserId() {
        return userId;
    }

    public long getCouponId() {
        return couponId;
    }

    public String getCode() {
        return code;
    }
}
