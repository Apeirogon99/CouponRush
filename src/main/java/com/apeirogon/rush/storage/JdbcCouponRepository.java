package com.apeirogon.rush.storage;

import com.apeirogon.rush.domain.Coupon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JdbcCouponRepository implements CouponRepository {
    private final JdbcTemplate jdbc;

    @Autowired
    public JdbcCouponRepository(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /**
     *  전체 쿠폰 조회
     */
    @Override
    public List<Coupon> findAll() {
        return jdbc.queryForStream(
                "SELECT id, total_quantity, issued_quantity FROM coupons",
                (rs, rowNum) -> new Coupon(
                        rs.getLong("id"),
                        rs.getInt("total_quantity"),
                        rs.getInt("issued_quantity")
                )
        ).toList();
    }

    /**
     * 쿠폰 조회
     */
    @Override
    public Optional<Coupon> findById(Long id) {
        return jdbc.queryForStream(
                "SELECT id, total_quantity, issued_quantity FROM coupons WHERE id = ?",
                (rs, rowNum) -> new Coupon(
                        rs.getLong("id"),
                        rs.getInt("total_quantity"),
                        rs.getInt("issued_quantity")
                ),
                id
        ).findFirst();
    }

    /**
     * 쿠폰 조회 (락)
     */
    @Override
    public Optional<Coupon> findByIdWithLock(Long id) {
        return jdbc.queryForStream(
                "SELECT id, total_quantity, issued_quantity FROM coupons WHERE id = ? FOR UPDATE",
                (rs, rowNum) -> new Coupon(
                        rs.getLong("id"),
                        rs.getInt("total_quantity"),
                        rs.getInt("issued_quantity")
                ),
                id
        ).findFirst();
    }

    /**
     * 쿠폰 발행 증가
     */
    @Override
    public int increaseIssuedQuantity(Long couponId) {
        return jdbc.update(
                "UPDATE coupons SET issued_quantity = issued_quantity + 1, updated_at = ? WHERE id = ? AND issued_quantity < total_quantity",
                LocalDateTime.now(),
                couponId
        );
    }

    /**
     * 쿠폰 생성
     */
    @Override
    public Coupon save(Coupon coupon) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO coupons (total_quantity, issued_quantity) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, coupon.getTotalQuantity());
            ps.setInt(2, coupon.getIssuedQuantity());
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKey().longValue();
        return new Coupon(id, coupon.getTotalQuantity(), coupon.getIssuedQuantity());
    }

    @Override
    public int deleteAll() {
        return jdbc.update("DELETE FROM coupons");
    }
}
