package com.apeirogon.rush;

import com.apeirogon.rush.domain.CacheCoupon;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("scenario2")
class RedisTests {

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper redisObjectMapper;

    @BeforeEach
    void setUp() {
        String key = "COUPON_KEY_1";
        CacheCoupon inValue = new CacheCoupon(key, 100);

        redisTemplate.delete(key);
        redisTemplate.opsForValue().set(key, inValue);
    }

    @Test
    public void insertTest() {
        String key = "COUPON_KEY_1";

        Object outObject = redisTemplate.opsForValue().get(key);
        CacheCoupon outValue = redisObjectMapper.convertValue(outObject, CacheCoupon.class);

        assertEquals("COUPON_KEY_1", outValue.getId());
        assertEquals(100, outValue.getQuantity());
    }

    @Test
    public void issuedQuantityTest() {
        String key = "COUPON_KEY_1";

        {
            Object outObject = redisTemplate.opsForValue().get(key);
            CacheCoupon outValue = redisObjectMapper.convertValue(outObject, CacheCoupon.class);

            outValue.setQuantity(outValue.getQuantity() - 1);
            redisTemplate.opsForValue().set(key, outValue);
        }

        {
            Object outObject = redisTemplate.opsForValue().get(key);
            CacheCoupon outValue = redisObjectMapper.convertValue(outObject, CacheCoupon.class);

            assertEquals("COUPON_KEY_1", outValue.getId());
            assertEquals(99, outValue.getQuantity());
        }
    }
}