package com.apeirogon.rush.domain;

import com.apeirogon.rush.domain.Strategy.CouponIssueStrategy;
import com.apeirogon.rush.domain.Strategy.DistributedStrategy;
import com.apeirogon.rush.domain.Strategy.MessagingStrategy;
import com.apeirogon.rush.domain.Strategy.PessimisticLockStrategy;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 쿠폰 서비스
 */
@Service
public class CouponService {
    // scenario #1 (DB)
    private final PessimisticLockStrategy pessimisticLockStrategy;      // 비관적락 전략

    // scenario #2 (DB + Redis)
    private final DistributedStrategy distributedStrategy;              // 분산락 전략

    // scenario #3 (DB + Redis + Kafka)
    private final MessagingStrategy messagingStrategy;                  // 메세지 전략

    @Autowired
    public CouponService(
            @Nullable PessimisticLockStrategy pessimisticLockStrategy,
            @Nullable DistributedStrategy distributedStrategy,
            @Nullable MessagingStrategy messagingStrategy
            ) {
        this.pessimisticLockStrategy = pessimisticLockStrategy;
        this.distributedStrategy = distributedStrategy;
        this.messagingStrategy = messagingStrategy;
    }

    /**
     *  쿠폰 생성
     */
    public void initCoupon(Long couponId, Integer quantity, boolean useCache, boolean useMessaging) {
        CouponIssueStrategy strategy = getCouponIssueStrategy(useCache, useMessaging);
        strategy.initCoupon(couponId, quantity);
    }

    /**
     *  쿠폰 발급
     */
    public void issueCoupon(long couponId, long userId, boolean useCache, boolean useMessaging) {
        CouponIssueStrategy strategy = getCouponIssueStrategy(useCache, useMessaging);
        strategy.issueCoupon(couponId, userId);
    }

    /**
     *  특정 쿠폰 조회
     */
    public Coupon getCoupon(long couponId, boolean useCache, boolean useMessaging) {
        CouponIssueStrategy strategy = getCouponIssueStrategy(useCache, useMessaging);
        return strategy.getCoupon(couponId);
    }

    /**
     *  시나리오에 따른 전략
     */
    private CouponIssueStrategy getCouponIssueStrategy(boolean useCache, boolean useMessaging) {
        if (!useCache && !useMessaging) {
            return pessimisticLockStrategy;
        } else if (useCache && !useMessaging) {
            return distributedStrategy;
        } else if (useCache && useMessaging) {
            return messagingStrategy;
        }
        return null;
    }

}
