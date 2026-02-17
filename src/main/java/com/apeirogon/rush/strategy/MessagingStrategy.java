package com.apeirogon.rush.strategy;

import com.apeirogon.rush.api.controller.response.CouponResponse;
import com.apeirogon.rush.api.controller.response.CreateCouponResponse;
import com.apeirogon.rush.api.controller.response.IssueCouponResponse;
import com.apeirogon.rush.domain.Coupon;
import com.apeirogon.rush.messaging.CouponIssueEvent;
import com.apeirogon.rush.storage.JdbcCouponRepository;
import com.apeirogon.rush.storage.JdbcIssuedCouponRepository;
import com.apeirogon.rush.support.error.CoreException;
import com.apeirogon.rush.support.error.ErrorType;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * scenario #3 ( DB + Redis + Kafka )
 * Redis : 중복 체크, 쿠폰 발급
 * Kafka : 발급 이벤트 publish
 * Consumer : 비동기 DB 저장
 */
@Service
@Profile("scenario3")
public class MessagingStrategy implements CouponIssueStrategy {

    private final JdbcCouponRepository jdbcCouponRepository;
    private final JdbcIssuedCouponRepository jdbcIssuedCouponRepository;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public MessagingStrategy(
            JdbcCouponRepository jdbcCouponRepository,
            JdbcIssuedCouponRepository jdbcIssuedCouponRepository,
            RedisTemplate<String, Object> redisTemplate,
            RedissonClient redissonClient,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.jdbcCouponRepository = jdbcCouponRepository;
        this.jdbcIssuedCouponRepository = jdbcIssuedCouponRepository;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CreateCouponResponse createCoupon(Integer quantity) {
        jdbcIssuedCouponRepository.deleteAll();
        jdbcCouponRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        Coupon coupon = new Coupon(null, quantity, 0);
        Coupon saved = jdbcCouponRepository.save(coupon);

        String key = "COUPON:" + saved.getId();
        redisTemplate.opsForValue().set(key, quantity, Duration.ofDays(1));

        return new CreateCouponResponse(saved.getId());
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void syncCouponQuantity() {
        List<Coupon> coupons = jdbcCouponRepository.findAll();

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
    public IssueCouponResponse issueCoupon(Long couponId, Long userId) {
        final String couponKey = "COUPON:" + couponId;
        final String userCouponSetKey = "ISSUED:" + couponKey;
        final String lockKey = "LOCK:USER:" + userId + ":COUPON:" + couponId;

        // 사전 검증 (fast-fail)
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(userCouponSetKey, userId.toString()))) {
            throw new CoreException(ErrorType.COUPON_ALREADY_ISSUED);
        }

        Integer remaining = (Integer) redisTemplate.opsForValue().get(couponKey);
        if (remaining == null) {
            throw new CoreException(ErrorType.COUPON_NOT_FOUND);
        }
        if (remaining <= 0) {
            throw new CoreException(ErrorType.COUPON_SOLD_OUT);
        }

        // 분산락 획득
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                throw new CoreException(ErrorType.LOCK_ACQUISITION_FAILED);
            }

            // 락 안에서 중복 재확인
            if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(userCouponSetKey, userId.toString()))) {
                throw new CoreException(ErrorType.COUPON_ALREADY_ISSUED);
            }

            // 재고 차감
            Long quantity = redisTemplate.opsForValue().decrement(couponKey);
            if (quantity == null) {
                throw new CoreException(ErrorType.COUPON_NOT_FOUND);
            }
            if (quantity < 0) {
                redisTemplate.opsForValue().increment(couponKey);
                throw new CoreException(ErrorType.COUPON_SOLD_OUT);
            }

            // 발급 기록
            redisTemplate.opsForSet().add(userCouponSetKey, userId.toString());
            redisTemplate.expire(userCouponSetKey, Duration.ofDays(1));

        } catch (InterruptedException e) {
            throw new CoreException(ErrorType.LOCK_ACQUISITION_FAILED);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        // Kafka로 발급 이벤트 publish
        CouponIssueEvent event = new CouponIssueEvent(userId, couponId, couponKey, userCouponSetKey);
        kafkaTemplate.send("coupon-issue-events", event)
                .exceptionally(ex -> {
                    redisTemplate.opsForValue().increment(couponKey);
                    redisTemplate.opsForSet().remove(userCouponSetKey, userId.toString());
                    return null;
                });

        return new IssueCouponResponse(1);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCoupons() {
        return new CouponResponse(jdbcCouponRepository.findAll());
    }
}
