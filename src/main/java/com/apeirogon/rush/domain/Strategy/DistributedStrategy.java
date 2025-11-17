package com.apeirogon.rush.domain.Strategy;

import com.apeirogon.rush.domain.Coupon;
import com.apeirogon.rush.domain.IssuedCoupon;
import com.apeirogon.rush.storage.CouponRepository;
import com.apeirogon.rush.storage.IssuedCouponRepository;
import com.apeirogon.rush.support.error.CoreException;
import com.apeirogon.rush.support.error.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * scenario #2 (DB + Redis)
 * Redis : 중복 체크, 쿠폰 발급
 * DB : 발급 저장 (비동기)
 */
@Service
@ConditionalOnProperty(name = "app.features.cache", havingValue = "true")
public class DistributedStrategy implements CouponIssueStrategy {

    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;
    private final RedissonClient redissonClient;

    @Autowired
    public DistributedStrategy(
            CouponRepository couponRepository,
            IssuedCouponRepository issuedCouponRepository,

            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper redisObjectMapper,
            RedissonClient redissonClient
    ) {
        this.couponRepository = couponRepository;
        this.issuedCouponRepository = issuedCouponRepository;

        this.redisTemplate = redisTemplate;
        this.redisObjectMapper = redisObjectMapper;
        this.redissonClient = redissonClient;
    }

    @Override
    public void initCoupon(Long couponId, Integer quantity) {
        Coupon coupon = new Coupon(couponId, quantity);
        couponRepository.save(coupon);

        String key = "COUPON:" + couponId;
        redisTemplate.opsForValue().set(key, quantity, Duration.ofSeconds(30));
    }

    @Scheduled(fixedRate = 5000)
    public void syncCouponQuantity() {

    }

    @Override
    public void issueCoupon(Long couponId, Long userId) {
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

    @Override
    @Transactional(readOnly = true)
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId);
    }
}
