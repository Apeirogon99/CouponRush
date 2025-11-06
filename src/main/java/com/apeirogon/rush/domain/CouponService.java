package com.apeirogon.rush.domain;

import com.apeirogon.rush.storage.CouponRepository;
import com.apeirogon.rush.storage.IssuedCouponRepository;
import com.apeirogon.rush.support.error.CoreException;
import com.apeirogon.rush.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 *  쿠폰 서비스
 */
@Service
public class CouponService {
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    @Autowired
    public CouponService(CouponRepository couponRepository,  IssuedCouponRepository issuedCouponRepository) {
        this.couponRepository = couponRepository;
        this.issuedCouponRepository = issuedCouponRepository;
    }

    /**
     * 쿠폰 생성
     */
    public void initCoupon(Long couponId, Integer quantity) {
        Coupon coupon = new Coupon(couponId, quantity);
        couponRepository.save(coupon);
    }

    /**
     * 특정 쿠폰 조회
     */
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId);
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
            throw new RuntimeException("이미 쿠폰을 발급 받음");
        }

        // 쿠폰 소진 확인
        if(coupon.getIssuedQuantity() >= coupon.getTotalQuantity()){
            throw new RuntimeException("쿠폰이 모두 소진됨");
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
