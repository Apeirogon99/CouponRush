package com.apeirogon.rush.messaging;

import com.apeirogon.rush.domain.IssuedCoupon;
import com.apeirogon.rush.storage.JdbcCouponRepository;
import com.apeirogon.rush.storage.JdbcIssuedCouponRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.features.messaging", havingValue = "true")
public class CouponIssueConsumer {

    private final JdbcCouponRepository jdbcCouponRepository;
    private final JdbcIssuedCouponRepository jdbcIssuedCouponRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public CouponIssueConsumer(
            JdbcCouponRepository jdbcCouponRepository,
            JdbcIssuedCouponRepository jdbcIssuedCouponRepository,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.jdbcCouponRepository = jdbcCouponRepository;
        this.jdbcIssuedCouponRepository = jdbcIssuedCouponRepository;
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "coupon-issue-events", groupId = "coupon-issue-consumer")
    @Transactional
    public void consume(CouponIssueEvent event) {
        try {
            if (jdbcIssuedCouponRepository.existsByUserIdAndCouponId(event.userId(), event.couponId())) {
                redisTemplate.opsForValue().increment(event.couponKey());
                redisTemplate.opsForSet().remove(event.userCouponSetKey(), event.userId().toString());
                return;
            }

            int updated = jdbcCouponRepository.increaseIssuedQuantity(event.couponId());
            if (updated == 0) {
                redisTemplate.opsForValue().increment(event.couponKey());
                redisTemplate.opsForSet().remove(event.userCouponSetKey(), event.userId().toString());
                redisTemplate.opsForValue().set(event.couponKey(), 0);
                return;
            }

            jdbcIssuedCouponRepository.save(new IssuedCoupon(event.userId(), event.couponId()));

        } catch (Exception e) {
            redisTemplate.opsForValue().increment(event.couponKey());
            redisTemplate.opsForSet().remove(event.userCouponSetKey(), event.userId().toString());
        }
    }
}
