package com.apeirogon.rush.Strategy;

import com.apeirogon.rush.domain.Coupon;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


/**
 * scenario #2 ( DB + Redis + Kafka )
 * Redis : 중복 체크, 쿠폰 발급
 * DB : 발급 저장 (비동기)
 */
@Service
@ConditionalOnProperty(name = "app.features.messaging", havingValue = "true")
public class MessagingStrategy implements CouponIssueStrategy {

    @Override
    public void initCoupon(Long couponId, Integer quantity) {

    }

    @Override
    public void issueCoupon(Long couponId, Long userId) {

    }

    @Override
    public Coupon getCoupon(Long couponId) {
        return null;
    }
}
