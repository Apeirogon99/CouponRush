package com.apeirogon.rush.domain;

import com.apeirogon.rush.storage.IssuedCouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *  발급된 쿠폰 서비스
 */
@Service
public class IssuedCouponService {
    private final IssuedCouponRepository issuedCouponRepository;

    @Autowired
    public IssuedCouponService(IssuedCouponRepository issuedCouponRepository) {
        this.issuedCouponRepository = issuedCouponRepository;
    }

    /**
     *  유저에게 발급된 쿠폰
     */
    public void getIssuedCoupon(Long userId) {

    }
}
