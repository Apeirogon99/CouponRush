package com.apeirogon.rush.api.controller.response;

import com.apeirogon.rush.domain.Coupon;

import java.util.List;

public record CouponResponse(
        List<Coupon> coupons
) {

}
