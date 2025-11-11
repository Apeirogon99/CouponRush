package com.apeirogon.rush.domain;

import com.apeirogon.rush.storage.CouponRepository;
import com.apeirogon.rush.storage.IssuedCouponRepository;
import com.apeirogon.rush.support.error.CoreException;
import com.apeirogon.rush.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 *  쿠폰 서비스
 */
@Service
public class CouponService {
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    @Autowired(required = false)
    public CouponService(CouponRepository couponRepository,  IssuedCouponRepository issuedCouponRepository) {
        this.couponRepository = couponRepository;
        this.issuedCouponRepository = issuedCouponRepository;
    }

    /**
     * 쿠폰 생성
     */
    public void initCoupon(Long couponId, Integer quantity) {
        try {
            Coupon coupon = new Coupon(couponId, quantity);
            couponRepository.save(coupon);
        } catch (Exception ex) {
            throw new CoreException(ErrorType.COUPON_FAIL_REGISTER);
        }
    }

    /**
     * 특정 쿠폰 조회
     */
    public Coupon getCoupon(Long couponId) {
        try {
            return couponRepository.findById(couponId);
        } catch (Exception ex) {
            throw new CoreException(ErrorType.COUPON_NOT_FOUND);
        }
    }

    /**
     * 쿠폰 발급 (Wait Lock)
     * 쿠폰 아이디를 기준으로 쿠폰 발급
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 1)
    public void issueCouponWithLock(long userId, long couponId) {

        // 쿠폰 조회 및 락
        Coupon coupon = couponRepository.findByIdWithLockWait(couponId);
        if(coupon == null){
            throw new CoreException(ErrorType.COUPON_NOT_FOUND);
        }

        // 중복 발급 확인
        boolean isDuplicated = issuedCouponRepository.duplicateIssued(userId, couponId);
        if(isDuplicated){
            throw new CoreException(ErrorType.COUPON_ALREADY_ISSUED);
        }

        // 쿠폰 소진 확인
        if(coupon.getIssuedQuantity() >= coupon.getTotalQuantity()){
            throw new CoreException(ErrorType.COUPON_SOLD_OUT);
        }

        // 쿠폰 발급
        couponRepository.increaseIssuedQuantity(couponId);
        
        // 쿠폰 발급 테이블에 저장
        issuedCouponRepository.save(
                new IssuedCoupon(
                        userId,
                        couponId
                )
        );
    }

    /**
     * 쿠폰 발급 (Wait Update)
     * 쿠폰 아이디를 기준으로 쿠폰 발급
     */
    @Transactional(timeout = 1)
    public void issueCouponWithUpdate(long userId, long couponId) {

        int updated = couponRepository.increaseIssuedQuantityWithCheck(couponId);
        if(updated <= 0){
            throw new CoreException(ErrorType.DEFAULT_ERROR);
        }

        boolean isDuplicated = issuedCouponRepository.duplicateIssued(userId, couponId);
        if(isDuplicated){
            throw new CoreException(ErrorType.DEFAULT_ERROR);
        }

        issuedCouponRepository.save(
                new IssuedCoupon(
                        userId,
                        couponId
                )
        );
    }
}
