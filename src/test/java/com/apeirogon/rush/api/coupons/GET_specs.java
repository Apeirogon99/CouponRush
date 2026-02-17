package com.apeirogon.rush.api.coupons;

import com.apeirogon.rush.api.CouponApiTest;
import com.apeirogon.rush.api.TestFixture;
import com.apeirogon.rush.api.controller.response.CouponResponse;
import com.apeirogon.rush.support.response.ApiResult;
import com.apeirogon.rush.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import static org.assertj.core.api.Assertions.assertThat;

@CouponApiTest
@DisplayName("GET /coupons")
public class GET_specs {

    @Test
    void 올바르게_요청하면_SUCCESS를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act
        ApiResult<CouponResponse> result = fixture.get(
                "/coupons",
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(result.getResult()).isEqualTo(ResultType.SUCCESS);
    }

    @Test
    void 쿠폰이_없으면_빈_리스트를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act
        ApiResult<CouponResponse> result = fixture.get(
                "/coupons",
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(result.getData().coupons()).isEmpty();
    }

    @Test
    void 쿠폰이_있으면_쿠폰_목록을_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange
        fixture.createCoupon();
        fixture.createCoupon();

        // Act
        ApiResult<CouponResponse> result = fixture.get(
                "/coupons",
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(result.getData().coupons()).hasSizeGreaterThanOrEqualTo(2);
    }
}
