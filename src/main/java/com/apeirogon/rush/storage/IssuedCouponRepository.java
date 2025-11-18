package com.apeirogon.rush.storage;

import com.apeirogon.rush.domain.Coupon;
import com.apeirogon.rush.domain.IssuedCoupon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

@Repository
public class IssuedCouponRepository {
    private final JdbcTemplate jdbc;

    @Autowired
    public IssuedCouponRepository(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /**
     *
     */
    public Long countByUserIdAndCouponId(Integer userId, Integer couponId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM coupon_issues WHERE user_id = ? AND coupon_id = ?) GROUP BY user_id",
                Long.class,
                userId,
                couponId
        );
    }

    /**
     *  중복 발급 확인
     */
    public boolean existsByUserIdAndCouponId(Long userId, Long couponId) {
        Boolean ret = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM coupon_issues WHERE user_id = ? AND coupon_id = ?)",
                Boolean.class,
                userId,
                couponId
        );

        return Boolean.TRUE.equals(ret);
    }

    /**
     *  발급 쿠폰 저장
     */
    public void save(IssuedCoupon couponIssue) {
        jdbc.update(
                "INSERT INTO coupon_issues (user_id, coupon_id) VALUES (?, ?)",
                couponIssue.getUserId(),
                couponIssue.getCouponId()
        );
    }

    /**
     *  전체 삭제
     */
    public void deleteAll() {
        jdbc.update("DELETE FROM coupon_issues");
    }
}
