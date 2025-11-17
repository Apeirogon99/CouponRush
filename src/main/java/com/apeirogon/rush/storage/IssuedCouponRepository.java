package com.apeirogon.rush.storage;

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

    public boolean existsByUserIdAndCouponId(Long userId, Long couponId) {
        Boolean ret = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM coupon_issues WHERE user_id = ? AND coupon_id = ?)",
                Boolean.class,
                userId,
                couponId
        );

        return Boolean.TRUE.equals(ret);
    }

    public void save(IssuedCoupon couponIssue) {
        jdbc.update(
                "INSERT INTO coupon_issues (user_id, coupon_id) VALUES (?, ?)",
                couponIssue.getUserId(),
                couponIssue.getCouponId()
        );
    }
}
