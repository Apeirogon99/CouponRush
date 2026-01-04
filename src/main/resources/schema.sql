DROP TABLE IF EXISTS coupon_issues;
DROP TABLE IF EXISTS coupons;

-- 쿠폰 테이블
CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '아이디',
    total_quantity INT NOT NULL COMMENT '전체 수량',
    issued_quantity INT NOT NULL DEFAULT 0 COMMENT '발급 수량',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    CONSTRAINT check_quantity CHECK (issued_quantity <= total_quantity AND issued_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='쿠폰 테이블';

-- 쿠폰 발급 테이블
CREATE TABLE coupon_issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '아이디',
    coupon_id BIGINT NOT NULL COMMENT '쿠폰 아이디',
    user_id BIGINT NOT NULL COMMENT '유저 아이디',
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급일자',
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    UNIQUE KEY uk_user_coupon (user_id, coupon_id),
    INDEX idx_coupon_issues_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='쿠폰 발급 테이블';