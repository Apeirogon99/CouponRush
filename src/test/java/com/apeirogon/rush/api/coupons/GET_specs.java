package com.apeirogon.rush.api.coupons;

import com.apeirogon.rush.api.CouponApiTest;
import com.apeirogon.rush.api.TestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CouponApiTest
@DisplayName("GET /coupons")
public class GET_specs {

    @Test
    void 올바르게_요청하면_200_OK_상태코드를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void 쿠폰이_없으면_빈_리스트를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void 쿠폰이_있으면_쿠폰_목록을_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act

        // Assert
    }
}
