package com.apeirogon.rush.storage;

import com.apeirogon.rush.domain.Coupon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class CouponRepository {
    private final JdbcTemplate jdbc;

    @Autowired
    public CouponRepository(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /**
     *  전체 쿠폰 조회
     */
    public List<Coupon> findAll() {
        return jdbc.query(
                "SELECT id, total_quantity, issued_quantity FROM coupons",
                (rs, rowNum) -> new Coupon(
                        rs.getLong("id"),
                        rs.getInt("total_quantity"),
                        rs.getInt("issued_quantity")
                )
        );
    }

    /**
     *  특정 쿠폰 조회
     */
    public Coupon findById(long id) {
        List<Coupon> coupons = jdbc.query(
                "SELECT id, total_quantity, issued_quantity FROM coupons WHERE id = ?",
                (rs, rowNum) -> new Coupon(
                        rs.getLong("id"),
                        rs.getInt("total_quantity"),
                        rs.getInt("issued_quantity")
                ),
                id
        );
        return coupons.isEmpty() ? null : coupons.get(0);
    }

    /**
     * 쿠폰 조회 (락)
     */
    public Coupon findByIdWithLockWait(long id) {
        List<Coupon> coupons = jdbc.query(
                "SELECT id, total_quantity, issued_quantity FROM coupons WHERE id = ? FOR UPDATE",
                (rs, rowNum) -> new Coupon(
                        rs.getLong("id"),
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
    public int increaseIssuedQuantity(Long couponId) {
        return jdbc.update(
                "UPDATE coupons SET issued_quantity = issued_quantity + 1, updated_at = ? WHERE id = ? AND issued_quantity <= total_quantity",
                LocalDateTime.now(),
                couponId
        );
    }

    /**
     * 쿠폰 저장
     */
    public void save(Coupon coupon) {

        int updatedRows = jdbc.update(
                "UPDATE coupons SET total_quantity = ?, issued_quantity = ? WHERE id = ?",
                coupon.getTotalQuantity(),
                coupon.getIssuedQuantity(),
                coupon.getId()
        );

        if (updatedRows == 0) {
            jdbc.update(
                    "INSERT INTO coupons (id, total_quantity, issued_quantity) VALUES (?, ?, ?)",
                    coupon.getId(),
                    coupon.getTotalQuantity(),
                    coupon.getIssuedQuantity()
            );
        }
    }

    /**
     *  전체 삭제
     */
    public void deleteAll() {
        jdbc.update("DELETE FROM coupons");
    }
}
