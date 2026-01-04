package com.apeirogon.rush.storage;

import com.apeirogon.rush.domain.Coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    List<Coupon> findAll();
    Optional<Coupon> findById(Long id);
    Optional<Coupon> findByIdWithLock(Long id);
    int increaseIssuedQuantity(Long id);
    Coupon save(Coupon coupon);
    int deleteAll();
}
