package com.apeirogon.rush.api.controller;

import com.apeirogon.rush.api.controller.request.CreateCouponRequest;
import com.apeirogon.rush.api.controller.request.IssueCouponRequest;
import com.apeirogon.rush.api.controller.response.CouponResponse;
import com.apeirogon.rush.api.controller.response.CreateCouponResponse;
import com.apeirogon.rush.api.controller.response.IssueCouponResponse;
import com.apeirogon.rush.strategy.CouponIssueStrategy;
import com.apeirogon.rush.support.response.ApiResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class CouponController {
    private final CouponIssueStrategy couponIssueStrategy;

    @Autowired
    public CouponController(CouponIssueStrategy couponIssueStrategy) {
        this.couponIssueStrategy = couponIssueStrategy;
    }

    /**
     * 쿠폰 생성
     */
    @PostMapping("/coupons")
    public ApiResult<CreateCouponResponse> createCoupon(@RequestBody CreateCouponRequest request) {
        return ApiResult.success(couponIssueStrategy.createCoupon(request.quantity()));
    }

    /**
     * 쿠폰 발급
     */
    @PostMapping("/coupons/{couponId}/issues")
    public ApiResult<IssueCouponResponse> issueCoupon(@PathVariable Long couponId, @RequestBody IssueCouponRequest request) {
        return ApiResult.success(couponIssueStrategy.issueCoupon(couponId, request.userId()));
    }

    /**
     * 모든 쿠폰 조회
     */
    @GetMapping("/coupons")
    public ApiResult<CouponResponse> getCoupons() {
        return ApiResult.success(couponIssueStrategy.getCoupons());
    }
}
