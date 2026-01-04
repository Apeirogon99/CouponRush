package com.apeirogon.rush.storage;

import com.apeirogon.rush.domain.IssuedCoupon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

@Repository
public class JdbcIssuedCouponRepository implements IssuedCouponRepository {
    private final JdbcTemplate jdbc;

    @Autowired
    public JdbcIssuedCouponRepository(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /**
     *  중복 발급 확인
     */
    @Override
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
    @Override
    public void save(IssuedCoupon issuedCoupon) {
        jdbc.update(
                "INSERT INTO coupon_issues (user_id, coupon_id) VALUES (?, ?)",
                issuedCoupon.userId(),
                issuedCoupon.couponId()
        );
    }

    @Override
    public int deleteAll() {
        return jdbc.update("DELETE FROM coupon_issues");
    }
}
