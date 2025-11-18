package com.apeirogon.rush.Strategy;

import com.apeirogon.rush.domain.Coupon;

public interface CouponIssueStrategy {
    void initCoupon(Long couponId, Integer quantity);
    void issueCoupon(Long couponId, Long userId);
    Coupon getCoupon(Long couponId);
}
