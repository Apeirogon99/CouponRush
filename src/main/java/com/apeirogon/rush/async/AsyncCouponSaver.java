package com.apeirogon.rush.async;

import com.apeirogon.rush.domain.IssuedCoupon;
import com.apeirogon.rush.storage.JdbcCouponRepository;
import com.apeirogon.rush.storage.JdbcIssuedCouponRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.features.cache", havingValue = "true")
public class AsyncCouponSaver {

    private final JdbcCouponRepository jdbcCouponRepository;
    private final JdbcIssuedCouponRepository jdbcIssuedCouponRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public AsyncCouponSaver(
            JdbcCouponRepository jdbcCouponRepository,
            JdbcIssuedCouponRepository jdbcIssuedCouponRepository,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.jdbcCouponRepository = jdbcCouponRepository;
        this.jdbcIssuedCouponRepository = jdbcIssuedCouponRepository;
        this.redisTemplate = redisTemplate;
    }

    @Async
    @Transactional
    public void save(Long userId, Long couponId, String couponKey, String userCouponSetKey) {
        try {
            if (jdbcIssuedCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
                redisTemplate.opsForValue().increment(couponKey);
                redisTemplate.opsForSet().remove(userCouponSetKey, userId.toString());
                return;
            }

            int updated = jdbcCouponRepository.increaseIssuedQuantity(couponId);
            if (updated == 0) {
                redisTemplate.opsForValue().increment(couponKey);
                redisTemplate.opsForSet().remove(userCouponSetKey, userId.toString());
                redisTemplate.opsForValue().set(couponKey, 0);
                return;
            }

            jdbcIssuedCouponRepository.save(new IssuedCoupon(userId, couponId));

        } catch (Exception e) {
            redisTemplate.opsForValue().increment(couponKey);
            redisTemplate.opsForSet().remove(userCouponSetKey, userId.toString());
        }
    }
}
