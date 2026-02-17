package com.apeirogon.rush.api.create;

import com.apeirogon.rush.api.CouponApiTest;
import com.apeirogon.rush.api.TestFixture;
import com.apeirogon.rush.api.controller.request.CreateCouponRequest;
import com.apeirogon.rush.api.controller.response.CreateCouponResponse;
import com.apeirogon.rush.support.response.ApiResult;
import com.apeirogon.rush.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import static org.assertj.core.api.Assertions.assertThat;

@CouponApiTest
@DisplayName("POST /coupons")
public class POST_specs {

    @Test
    void 올바르게_요청하면_SUCCESS를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange
        CreateCouponRequest request = new CreateCouponRequest(100);

        // Act
        ApiResult<CreateCouponResponse> result = fixture.post(
                "/coupons",
                request,
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(result.getResult()).isEqualTo(ResultType.SUCCESS);
    }

    @Test
    void 올바르게_요청하면_생성된_쿠폰_ID를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange
        CreateCouponRequest request = new CreateCouponRequest(50);

        // Act
        ApiResult<CreateCouponResponse> result = fixture.post(
                "/coupons",
                request,
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(result.getData().id()).isNotNull().isPositive();
    }

    @Test
    void quantity가_0이면_E400_에러를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange
        CreateCouponRequest request = new CreateCouponRequest(0);

        // Act
        ApiResult<?> result = fixture.post(
                "/coupons",
                request,
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(result.getResult()).isEqualTo(ResultType.ERROR);
        assertThat(result.getError().getCode()).isEqualTo("E400");
    }

    @Test
    void quantity가_음수이면_E400_에러를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange
        CreateCouponRequest request = new CreateCouponRequest(-1);

        // Act
        ApiResult<?> result = fixture.post(
                "/coupons",
                request,
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(result.getResult()).isEqualTo(ResultType.ERROR);
        assertThat(result.getError().getCode()).isEqualTo("E400");
    }

    @Test
    void quantity가_없으면_E400_에러를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange
        CreateCouponRequest request = new CreateCouponRequest(null);

        // Act
        ApiResult<?> result = fixture.post(
                "/coupons",
                request,
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(result.getResult()).isEqualTo(ResultType.ERROR);
        assertThat(result.getError().getCode()).isEqualTo("E400");
    }
}
