package com.apeirogon.rush.domain.Strategy;

import com.apeirogon.rush.domain.Coupon;

public interface CouponIssueStrategy {
    void initCoupon(Long couponId, Integer quantity);
    void issueCoupon(Long couponId, Long userId);
    Coupon getCoupon(Long couponId);
}
