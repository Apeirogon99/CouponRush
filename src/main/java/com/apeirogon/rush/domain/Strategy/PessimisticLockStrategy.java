package com.apeirogon.rush.domain.Strategy;

import com.apeirogon.rush.domain.Coupon;
import com.apeirogon.rush.domain.IssuedCoupon;
import com.apeirogon.rush.storage.CouponRepository;
import com.apeirogon.rush.storage.IssuedCouponRepository;
import com.apeirogon.rush.support.error.CoreException;
import com.apeirogon.rush.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 *  scenario #1 (DB)
 *  DB : 중복 확인, 쿠폰 발급, 발급 저장
 */
@Service
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
    public void initCoupon(Long couponId, Integer quantity) {
        Coupon coupon = new Coupon(couponId, quantity);
        couponRepository.save(coupon);

    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 1)
    public void issueCoupon(Long couponId, Long userId) {

        // 중복 발급 확인
        if (issuedCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new CoreException(ErrorType.COUPON_ALREADY_ISSUED);
        }

        // 쿠폰 조회 및 락
        Coupon coupon = couponRepository.findByIdWithLockWait(couponId);
        if (coupon == null) {
            throw new CoreException(ErrorType.COUPON_NOT_FOUND);
        }

        // 쿠폰 소진 확인
        if (coupon.isSoldOut()) {
            throw new CoreException(ErrorType.COUPON_SOLD_OUT);
        }

        // 쿠폰 발급
        int updated = couponRepository.increaseIssuedQuantity(couponId);
        if(updated == 0) {
            throw new CoreException(ErrorType.COUPON_SOLD_OUT);
        }

        // 쿠폰 발급 테이블에 저장
        issuedCouponRepository.save(new IssuedCoupon(userId, couponId));

    }

    @Override
    @Transactional(readOnly = true)
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId);
    }
}
