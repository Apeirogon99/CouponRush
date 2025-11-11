package com.apeirogon.rush.controller;

import com.apeirogon.rush.domain.Coupon;
import com.apeirogon.rush.domain.CouponService;
import com.apeirogon.rush.support.response.ApiResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CouponController {
    private final CouponService couponService;

    @Autowired
    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * 특정 쿠폰 초기화
     */
    @GetMapping("/{couponId}/init")
    public ApiResult<String> initCoupon(@PathVariable Long couponId, @RequestParam Integer quantity) {
        couponService.initCoupon(couponId, quantity);
        return ApiResult.success("쿠폰이 생성되었습니다.");
    }

    /**
     * 쿠폰 발급
     */
    @PostMapping("{couponId}/issue")
    public ApiResult<String> issueCoupon(@PathVariable Long couponId, @RequestParam Long userId) {
        couponService.issueCouponWithLock(userId, couponId);
        return ApiResult.success("쿠폰을 발급되었습니다.");
    }

    /**
     * 특정 쿠폰 조회
     */
    @GetMapping("/coupons/{couponId}")
    public ApiResult<Coupon> getCoupon(@PathVariable Long couponId) {
        return ApiResult.success(couponService.getCoupon(couponId));
    }
}
