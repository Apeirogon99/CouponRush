package com.apeirogon.rush.storage;

import com.apeirogon.rush.domain.Coupon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class CouponRepository {
    private final JdbcTemplate jdbc;

    @Autowired
    public CouponRepository(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public List<Coupon> findAll() {
        return jdbc.query(
                "SELECT id, name, total_quantity, issued_quantity FROM coupons",
                (rs, rowNum) -> new Coupon(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getInt("total_quantity"),
                        rs.getInt("issued_quantity")
                )
        );
    }

    public Coupon findById(long id) {
        List<Coupon> coupons = jdbc.query(
                "SELECT id, name, total_quantity, issued_quantity FROM coupons WHERE id = ?",
                (rs, rowNum) -> new Coupon(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getInt("total_quantity"),
                        rs.getInt("issued_quantity")
                ),
                id
        );
        return coupons.isEmpty() ? null : coupons.get(0);
    }

    public Coupon findByIdWithLockWait(long id) {
        List<Coupon> coupons = jdbc.query(
                "SELECT id, name, total_quantity, issued_quantity FROM coupons WHERE id = ? FOR UPDATE",
                (rs, rowNum) -> new Coupon(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getInt("total_quantity"),
                        rs.getInt("issued_quantity")
                ),
                id
        );
        return coupons.isEmpty() ? null : coupons.get(0);
    }

    public Coupon findByIdWithLockNoWait(long id) {
        List<Coupon> coupons = jdbc.query(
                "SELECT id, name, total_quantity, issued_quantity FROM coupons WHERE id = ? FOR UPDATE NOWAIT",
                (rs, rowNum) -> new Coupon(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getInt("total_quantity"),
                        rs.getInt("issued_quantity")
                ),
                id
        );
        return coupons.isEmpty() ? null : coupons.get(0);
    }

    /**
     * 쿠폰 발행 증가
     */
    public void increaseIssuedQuantity(Long couponId) {
        jdbc.update(
                "UPDATE coupons SET issued_quantity = issued_quantity + 1, updated_at = ? WHERE id = ?",
                LocalDateTime.now(),
                couponId
        );
    }

    /**
     * 쿠폰 저장
     * 발급 수량과 업데이트만 저장
     */
    public void save(Coupon coupon) {
        jdbc.update(
                "UPDATE coupons SET issued_quantity = ?, updated_at = ? WHERE id = ?",
                coupon.getIssuedQuantity(),
                LocalDateTime.now(),
                coupon.getId()
        );
    }
}
