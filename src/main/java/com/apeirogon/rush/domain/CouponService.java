package com.apeirogon.rush.domain;

import com.apeirogon.rush.storage.CouponRepository;
import com.apeirogon.rush.storage.IssuedCouponRepository;
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
     * 모든 쿠폰 조회
     * 현재 존재하는 모든 쿠폰을 반환
     * (id, name, total quantity, issue quantity)
     */
    public List<Coupon> getAllCoupon() {
        return couponRepository.findAll();
    }

    /**
     * 쿠폰 발급
     * 쿠폰 아이디를 기준으로 쿠폰 발급
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 2)
    public void issueCouponWithLock(long userId, long couponId) {

        // 쿠폰 조회 및 락
        Coupon coupon = couponRepository.findByIdWithLockWait(couponId);
        if(coupon == null){
            throw new RuntimeException("쿠폰이 존재하지 않음");
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

        // 임의의 쿠폰 생성 코드
        String code = generatorCoupon();
        
        // 쿠폰 발급 테이블에 저장
        issuedCouponRepository.save(
                new IssuedCoupon(
                        userId,
                        couponId,
                        code
                )
        );
    }

    @Transactional
    public void issueCouponWithUpdate(long userId, long couponId) {

        // 쿠폰 조회 및 락
        Coupon coupon = couponRepository.findById(couponId);
        if(coupon == null){
            throw new RuntimeException("쿠폰이 존재하지 않음");
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

        // 업데이트로 발행 증가
        int update = couponRepository.increaseIssuedQuantityWithCheck(couponId);
        if(update <= 0){
            throw new RuntimeException("쿠폰이 모두 소진됨");
        }

        // 임의의 쿠폰 생성 코드
        String code = generatorCoupon();

        // 쿠폰 발급 테이블에 저장
        issuedCouponRepository.save(
                new IssuedCoupon(
                        userId,
                        couponId,
                        code
                )
        );
    }

    /**
     * 입장 코드 생성
     * XXXX-XXXX-XXXX-XXXX
     */
    private String generatorCoupon() {

        final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(19);
        for (int i = 0; i < 16; i++) {
            if (i > 0 && i % 4 == 0) sb.append('-');
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
