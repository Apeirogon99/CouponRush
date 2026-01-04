package com.apeirogon.rush.storage;

import com.apeirogon.rush.domain.IssuedCoupon;

public interface IssuedCouponRepository {
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
    void save(IssuedCoupon issuedCoupon);
    int deleteAll();
}
