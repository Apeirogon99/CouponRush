package com.apeirogon.rush.strategy;

import com.apeirogon.rush.api.controller.response.CouponResponse;
import com.apeirogon.rush.api.controller.response.CreateCouponResponse;
import com.apeirogon.rush.api.controller.response.IssueCouponResponse;
import com.apeirogon.rush.domain.Coupon;
import com.apeirogon.rush.domain.IssuedCoupon;
import com.apeirogon.rush.storage.CouponRepository;
import com.apeirogon.rush.storage.IssuedCouponRepository;
import com.apeirogon.rush.support.error.CoreException;
import com.apeirogon.rush.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * scenario #1 (DB)
 * DB : 중복 확인, 쿠폰 발급, 발급 저장
 */
@Service
@Profile("scenario1")
public class PessimisticLockStrategy implements CouponIssueStrategy {
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    @Autowired
    public PessimisticLockStrategy(
            CouponRepository couponRepository,
            IssuedCouponRepository issuedCouponRepository
    ) {
        this.couponRepository = couponRepository;
        this.issuedCouponRepository = issuedCouponRepository;
    }

    @Override
    @Transactional
    public CreateCouponResponse createCoupon(Integer quantity) {
        Coupon coupon = new Coupon(null, quantity, 0);
        Coupon saved = couponRepository.save(coupon);
        return new CreateCouponResponse(saved.id());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 3)
    public IssueCouponResponse issueCoupon(Long couponId, Long userId) {

        // 쿠폰 조회 및 락
        Optional<Coupon> coupon = couponRepository.findByIdWithLock(couponId);
        if (coupon.isEmpty()) {
            throw new CoreException(ErrorType.COUPON_NOT_FOUND);
        }

        // 중복 발급 확인
        if (issuedCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new CoreException(ErrorType.COUPON_ALREADY_ISSUED);
        }

        // 쿠폰 발급 및 소진 확인
        int updated = couponRepository.increaseIssuedQuantity(couponId);
        if (updated == 0) {
            throw new CoreException(ErrorType.COUPON_SOLD_OUT);
        }

        // 쿠폰 발급 테이블에 저장
        issuedCouponRepository.save(new IssuedCoupon(userId, couponId));
        return new IssueCouponResponse(1);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCoupons() {
        return new CouponResponse(couponRepository.findAll());
    }
}
