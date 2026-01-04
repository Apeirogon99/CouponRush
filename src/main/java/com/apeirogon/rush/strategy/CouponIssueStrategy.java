package com.apeirogon.rush.strategy;

import com.apeirogon.rush.api.controller.response.CouponResponse;
import com.apeirogon.rush.api.controller.response.CreateCouponResponse;
import com.apeirogon.rush.api.controller.response.IssueCouponResponse;

public interface CouponIssueStrategy {
    CreateCouponResponse createCoupon(Integer quantity);
    IssueCouponResponse issueCoupon(Long couponId, Long userId);
    CouponResponse getCoupons();
}
