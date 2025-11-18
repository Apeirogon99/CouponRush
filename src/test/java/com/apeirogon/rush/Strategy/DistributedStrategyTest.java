package com.apeirogon.rush.Strategy;

import com.apeirogon.rush.domain.Coupon;
import com.apeirogon.rush.domain.CouponService;
import com.apeirogon.rush.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("scenario2")
class DistributedStrategyTest {

    @Autowired
    private CouponService couponService;

    @BeforeEach
    void setUp() {

    }

    @Test
    @DisplayName("중복 발급 테스트")
    public void duplicatedTest() throws InterruptedException {
        Long userId = 1L;
        Long couponId = 1L;
        Integer couponQuantity = 100;
        Integer totalUsers = 100;

        // given
        couponService.initCoupon(couponId, couponQuantity, true, false);

        // when
        ExecutorService executorService = Executors.newFixedThreadPool(totalUsers);
        CountDownLatch latch = new CountDownLatch(totalUsers);

        for (int i = 0; i < totalUsers; i++) {
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(couponId, userId, true, false);
                } catch (CoreException e) {

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThrows(CoreException.class,
                () -> couponService.issueCoupon(couponId, userId, true, false));

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            Coupon coupon = couponService.getCoupon(couponId, true, false);
            assertEquals(1, coupon.getIssuedQuantity());
        });
    }

    @Test
    @DisplayName("정확하게 발급되는지 확인")
    public void exactTest() throws InterruptedException {
        Long couponId = 1L;
        Integer couponQuantity = 100;
        Integer totalUsers = 100;

        // given
        couponService.initCoupon(couponId, couponQuantity, true, false);

        // when
        ExecutorService executorService = Executors.newFixedThreadPool(totalUsers);
        CountDownLatch latch = new CountDownLatch(totalUsers);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < totalUsers; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(couponId, userId, true, false);
                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        System.out.println("Success: " + successCount.get() + ", Fail: " + failCount.get());

        // 비동기 처리 대기
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Coupon coupon = couponService.getCoupon(couponId, true, false);
            assertEquals(coupon.getIssuedQuantity(), coupon.getTotalQuantity(), "발급된 쿠폰 수가 일치하지 않음");
        });
    }
}