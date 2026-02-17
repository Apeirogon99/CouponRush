package com.apeirogon.rush.api.issue;

import com.apeirogon.rush.api.CouponApiTest;
import com.apeirogon.rush.api.TestFixture;
import com.apeirogon.rush.api.controller.request.IssueCouponRequest;
import com.apeirogon.rush.api.controller.response.CouponResponse;
import com.apeirogon.rush.api.controller.response.IssueCouponResponse;
import com.apeirogon.rush.domain.Coupon;
import com.apeirogon.rush.support.response.ApiResult;
import com.apeirogon.rush.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import static org.assertj.core.api.Assertions.assertThat;

@CouponApiTest
@DisplayName("POST /coupons/{couponId}/issues")
public class POST_specs {

    @Test
    void 올바르게_요청하면_SUCCESS를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange
        Long couponId = fixture.createCoupon();

        // Act
        ApiResult<IssueCouponResponse> result = fixture.post(
                "/coupons/" + couponId + "/issues",
                new IssueCouponRequest(1L, couponId),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(result.getResult()).isEqualTo(ResultType.SUCCESS);
    }

    @Test
    void 발급_후_쿠폰의_issuedQuantity가_1_증가한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange
        Long couponId = fixture.createCoupon();

        // Act
        fixture.issueCoupon(couponId, 1L);

        // Assert
        ApiResult<CouponResponse> result = fixture.get(
                "/coupons",
                new ParameterizedTypeReference<>() { }
        );
        Coupon actual = result.getData().coupons().stream()
                .filter(c -> c.id().equals(couponId))
                .findFirst()
                .orElseThrow();
        assertThat(actual.issuedQuantity()).isEqualTo(1);
    }

    @Test
    void 존재하지_않는_쿠폰이면_E404_에러를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange
        Long nonExistentCouponId = 999999L;

        // Act
        ApiResult<?> result = fixture.post(
                "/coupons/" + nonExistentCouponId + "/issues",
                new IssueCouponRequest(1L, nonExistentCouponId),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(result.getResult()).isEqualTo(ResultType.ERROR);
        assertThat(result.getError().getCode()).isEqualTo("E404");
    }

    @Test
    void 동일_사용자가_중복_발급하면_E409_에러를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange
        Long couponId = fixture.createCoupon();
        fixture.issueCoupon(couponId, 1L);

        // Act
        ApiResult<?> result = fixture.post(
                "/coupons/" + couponId + "/issues",
                new IssueCouponRequest(1L, couponId),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(result.getResult()).isEqualTo(ResultType.ERROR);
        assertThat(result.getError().getCode()).isEqualTo("E409");
    }

    @Test
    void 재고가_소진되면_E410_에러를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange
        Long couponId = fixture.createCoupon(1);
        fixture.issueCoupon(couponId, 1L);

        // Act
        ApiResult<?> result = fixture.post(
                "/coupons/" + couponId + "/issues",
                new IssueCouponRequest(2L, couponId),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(result.getResult()).isEqualTo(ResultType.ERROR);
        assertThat(result.getError().getCode()).isEqualTo("E410");
    }

    @Test
    void userId가_없으면_E400_에러를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange
        Long couponId = fixture.createCoupon();

        // Act
        ApiResult<?> result = fixture.post(
                "/coupons/" + couponId + "/issues",
                new IssueCouponRequest(null, couponId),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(result.getResult()).isEqualTo(ResultType.ERROR);
        assertThat(result.getError().getCode()).isEqualTo("E400");
    }
}
