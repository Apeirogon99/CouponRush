package com.apeirogon.rush.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    @GetMapping
    public String getAllCoupons() {
        return "get all coupons";
    }

    @PostMapping("/{couponId}")
    public String issueCoupon(@PathVariable String couponId) {
        return "success" + couponId;
    }
}
