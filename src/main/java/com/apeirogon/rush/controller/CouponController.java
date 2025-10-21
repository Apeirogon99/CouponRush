package com.apeirogon.rush.controller;

import com.apeirogon.rush.controller.v1.response.CouponInfoResponse;
import com.apeirogon.rush.controller.v1.response.CouponIssueResponse;
import com.apeirogon.rush.domain.Coupon;
import com.apeirogon.rush.domain.CouponService;
import com.apeirogon.rush.domain.IssuedCouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CouponController {
    private final CouponService couponService;
    private final IssuedCouponService issuedCouponService;

    @Autowired
    public CouponController(CouponService couponService, IssuedCouponService issuedCouponService) {
        this.couponService = couponService;
        this.issuedCouponService = issuedCouponService;
    }

    /**
     * 모든 쿠폰 조회
     */
    @GetMapping("/api/coupons")
    public List<CouponInfoResponse> getAllCoupons() {
        List<Coupon> coupons = couponService.getAllCoupon();

        return coupons.stream()
                .map(coupon -> new CouponInfoResponse(coupon.getId(), coupon.getName(), coupon.getTotalQuantity(), coupon.getIssuedQuantity()))
                .toList();
    }

    /**
     * 발급 받은 쿠폰 조회
     */
    @GetMapping("/api/coupons/{userId}")
    public List<CouponInfoResponse> getCoupon(Long userId) {
        List<Coupon> coupons = couponService.getAllCoupon();

        return coupons.stream()
                .map(coupon -> new CouponInfoResponse(coupon.getId(), coupon.getName(), coupon.getTotalQuantity(), coupon.getIssuedQuantity()))
                .toList();
    }

    /**
     * 쿠폰 발급
     */
    @PostMapping("/api/coupons/{couponId}/issue")
    public ResponseEntity<CouponIssueResponse> issueCoupon(@PathVariable Long couponId, @RequestParam Long userId) {
        try {
            couponService.issueCoupon(userId, couponId);
            return ResponseEntity.ok(new CouponIssueResponse(true, "구폰이 발급되었습니다."));
        } catch (RuntimeException e) {
            System.out.println("[Issue Coupon]" + e.getMessage());
            return ResponseEntity.ok(new CouponIssueResponse(false, e.getMessage()));
        }
    }
}
