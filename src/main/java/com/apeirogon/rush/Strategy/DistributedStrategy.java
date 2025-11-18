package com.apeirogon.rush.Strategy;

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
import java.util.List;
import java.util.Objects;
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
    private final RedissonClient redissonClient;

    @Autowired
    public DistributedStrategy(
            CouponRepository couponRepository,
            IssuedCouponRepository issuedCouponRepository,

            RedisTemplate<String, Object> redisTemplate,
            RedissonClient redissonClient
    ) {
        this.couponRepository = couponRepository;
        this.issuedCouponRepository = issuedCouponRepository;

        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
    }

    @Override
    public void initCoupon(Long couponId, Integer quantity) {
        issuedCouponRepository.deleteAll();
        couponRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        Coupon coupon = new Coupon(couponId, quantity);
        couponRepository.save(coupon);

        String key = "COUPON:" + couponId;
        redisTemplate.opsForValue().set(key, quantity, Duration.ofDays(1));
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void syncCouponQuantity() {
        List<Coupon> coupons = couponRepository.findAll();

        for (Coupon coupon : coupons) {
            String key = "COUPON:" + coupon.getId();
            Integer value = (Integer) redisTemplate.opsForValue().get(key);

            if (value == null) {
                int remaining = coupon.getTotalQuantity() - coupon.getIssuedQuantity();
                redisTemplate.opsForValue().set(key, remaining, Duration.ofDays(1));
            }
        }
    }

    @Override
    public void issueCoupon(Long couponId, Long userId) {
        final String couponKey = "COUPON:" + couponId;
        final String userCouponSetKey = "ISSUED:" + couponKey;
        final String lockKey = "LOCK:USER:" + userId + ":COUPON:" + couponId;

        RLock lock = redissonClient.getLock(lockKey);
        try {

            // 락 얻기 시도
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                throw new CoreException(ErrorType.LOCK_ACQUISITION_FAILED);
            }

            // 중복 체크
            if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(userCouponSetKey, userId.toString()))) {
                throw new CoreException(ErrorType.COUPON_ALREADY_ISSUED);
            }

            // 쿠폰 존재 확인
            Long quantity = redisTemplate.opsForValue().decrement(couponKey);
            if (quantity == null) {
                throw new CoreException(ErrorType.COUPON_NOT_FOUND);
            }

            // 재고 남았는지 확인
            if (quantity < 0) {
                redisTemplate.opsForValue().increment(couponKey);
                throw new CoreException(ErrorType.COUPON_SOLD_OUT);
            }

            redisTemplate.opsForSet().add(userCouponSetKey, userId.toString());
            redisTemplate.expire(userCouponSetKey, Duration.ofDays(1));

            asyncCouponSave(userId, couponId, couponKey, userCouponSetKey);

        } catch (InterruptedException e) {
            throw new CoreException(ErrorType.LOCK_ACQUISITION_FAILED);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Async
    @Transactional
    public void asyncCouponSave(Long userId, Long couponId, String couponKey, String userCouponKey) {

        try {
            // DB에서 중복 확인
            if (issuedCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {

                // 이미 발급 받았으므로 복구
                redisTemplate.opsForValue().increment(couponKey);
                redisTemplate.opsForSet().remove(userCouponKey, userId.toString());
                return;
            }

            // 쿠폰 발급
            int updated = couponRepository.increaseIssuedQuantity(couponId);
            if (updated == 0) {

                // 재고가 없으므로 복구
                redisTemplate.opsForValue().increment(couponKey);
                redisTemplate.opsForSet().remove(userCouponKey, userId.toString());
                redisTemplate.opsForValue().set(couponKey, 0);
                return;
            }

            // 쿠폰 발급 테이블에 저장
            issuedCouponRepository.save(new IssuedCoupon(userId, couponId));

        } catch (Exception e) {
            redisTemplate.opsForValue().increment(couponKey);
            redisTemplate.opsForSet().remove(userCouponKey, userId.toString());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId);
    }
}
