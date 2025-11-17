DROP TABLE IF EXISTS coupons CASCADE;
DROP TABLE IF EXISTS coupon_issues CASCADE;

-- 쿠폰 테이블
CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    total_quantity INT NOT NULL,
    issued_quantity INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_quantity CHECK (issued_quantity <= total_quantity AND issued_quantity >= 0)
);

COMMENT ON TABLE  coupons IS '쿠폰 테이블';
COMMENT ON COLUMN coupons.id IS '아이디';
COMMENT ON COLUMN coupons.total_quantity IS '전체 수량';
COMMENT ON COLUMN coupons.issued_quantity IS '발급 수량';
COMMENT ON COLUMN coupons.created_at IS '생성일자';
COMMENT ON COLUMN coupons.updated_at IS '수정일자';

-- 쿠폰 발급 테이블
CREATE TABLE coupon_issues (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    UNIQUE (user_id, coupon_id)
);

CREATE INDEX idx_coupon_issues_user_id ON coupon_issues(user_id);

COMMENT ON TABLE  coupon_issues IS '쿠폰 발급 테이블';
COMMENT ON COLUMN coupon_issues.id IS '아이디';
COMMENT ON COLUMN coupon_issues.coupon_id IS '쿠폰 아이디';
COMMENT ON COLUMN coupon_issues.user_id IS '유저 아이디';
COMMENT ON COLUMN coupon_issues.issued_at IS '발급일자';