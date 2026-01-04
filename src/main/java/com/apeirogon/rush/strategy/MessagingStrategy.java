package com.apeirogon.rush.strategy;

import com.apeirogon.rush.api.controller.response.CouponResponse;
import com.apeirogon.rush.api.controller.response.CreateCouponResponse;
import com.apeirogon.rush.api.controller.response.IssueCouponResponse;
import com.apeirogon.rush.domain.Coupon;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;


/**
 * scenario #2 ( DB + Redis + Kafka )
 * Redis : 중복 체크, 쿠폰 발급
 * DB : 발급 저장 (비동기)
 */
@Service
@Profile("scenario3")
public class MessagingStrategy implements CouponIssueStrategy {

    @Override
    public CreateCouponResponse createCoupon(Integer quantity) {
        return null;
    }

    @Override
    public IssueCouponResponse issueCoupon(Long couponId, Long userId) {
        return null;
    }

    @Override
    public CouponResponse getCoupons() {
        return null;
    }
}
