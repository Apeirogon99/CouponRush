package com.apeirogon.rush.domain;

import com.apeirogon.rush.domain.Strategy.CouponIssueStrategy;
import com.apeirogon.rush.domain.Strategy.PessimisticLockStrategy;
import com.apeirogon.rush.domain.Strategy.RedissonStrategy;
import com.apeirogon.rush.storage.CouponRepository;
import com.apeirogon.rush.storage.IssuedCouponRepository;
import com.apeirogon.rush.support.error.CoreException;
import com.apeirogon.rush.support.error.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 쿠폰 서비스
 */
@Service
public class CouponService {
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;
    private final RedissonClient redissonClient;

    @Autowired
    public CouponService(
            CouponRepository couponRepository,
            IssuedCouponRepository issuedCouponRepository,
            @Nullable RedisTemplate<String, Object> redisTemplate,
            @Nullable ObjectMapper redisObjectMapper,
            @Nullable RedissonClient redissonClient,
            ) {
        this.couponRepository = couponRepository;
        this.issuedCouponRepository = issuedCouponRepository;

        this.redisTemplate = redisTemplate;
        this.redisObjectMapper = redisObjectMapper;
        this.redissonClient = redissonClient;
    }

    /**
     * 쿠폰 생성
     */
    public void initCoupon(Long couponId, Integer quantity) {
        Coupon coupon = new Coupon(couponId, quantity);
        couponRepository.save(coupon);

        if (redisTemplate != null) {
            String key = "COUPON:" + couponId;
            redisTemplate.opsForValue().set(key, quantity, Duration.ofSeconds(30));
        }
    }

    /**
     * 특정 쿠폰 조회
     */
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId);
    }

    /**
     * 쿠폰 발급 (DB)
     * DB : 중복 확인, 쿠폰 발급, 발급 저장
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 1)
    public void issueCouponWithLock(long userId, long couponId) {

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
        if (updated == 0) {
            throw new CoreException(ErrorType.COUPON_SOLD_OUT);
        }

        // 쿠폰 발급 테이블에 저장
        issuedCouponRepository.save(new IssuedCoupon(userId, couponId));
    }

    /**
     * 쿠폰 발급 (DB + Redis)
     * Redis : 중복 체크, 쿠폰 발급
     * DB : 발급 저장 (비동기)
     */
    public void issueCouponWithRedis(long userId, long couponId) {

        String lockKey = "COUPON_LOCK_" + couponId;
        String couponKey = "COUPON_KEY_" + couponId;
        RLock lock = redissonClient.getLock(lockKey);
        try {

            // 락 얻기 시도
            if (!lock.tryLock(30, 10, TimeUnit.SECONDS)) {
                throw new CoreException(ErrorType.LOCK_ACQUISITION_FAILED);
            }

            // 중복 발급 확인
            if (issuedCouponRepository.duplicateIssued(userId, couponId)) {
                throw new CoreException(ErrorType.COUPON_ALREADY_ISSUED);
            }

            Object object = redisTemplate.opsForValue().get(couponKey);
            if (object == null) {
                throw new CoreException(ErrorType.COUPON_NOT_FOUND);
            }

            CacheCoupon cacheCoupon = redisObjectMapper.convertValue(object, CacheCoupon.class);
            if (cacheCoupon.getQuantity() <= 0) {
                throw new CoreException(ErrorType.COUPON_SOLD_OUT);
            }

            cacheCoupon.setQuantity(cacheCoupon.getQuantity() - 1);
            redisTemplate.opsForValue().set(couponKey, cacheCoupon);

            //asyncCouponSave.save(userId, couponId);

        } catch (CoreException e) {
            throw new CoreException(e.getErrorType());
        } catch (InterruptedException e) {
            throw new CoreException(ErrorType.LOCK_ACQUISITION_FAILED);
        } finally {
            if (lock != null && lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    public void syncCouponQuantity() {
        if (redisTemplate != null) {

        }
    }

    @Async
    @Transactional
    public void asyncCouponSave(Long userId, Long couponId) {

        if (!issuedCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            return;
        }

        int updated = couponRepository.increaseIssuedQuantity(couponId);
        if (updated == 0) {
            throw new CoreException(ErrorType.COUPON_SOLD_OUT);
        }

        issuedCouponRepository.save(new IssuedCoupon(userId, couponId));
    }

    /**
     * 쿠폰 발급 ( DB + REDIS + KAFKA )
     */
    public void issueCouponWithKafka(long userId, long couponId) {

    }

    public void issueCoupon(long couponId, long userId, boolean useCache, boolean useMessaging) {

    }

    private CouponIssueStrategy getCouponIssueStrategy(boolean useCache, boolean useMessaging) {

    }

}
