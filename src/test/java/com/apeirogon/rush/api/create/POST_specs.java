package com.apeirogon.rush.api.create;

import com.apeirogon.rush.api.CouponApiTest;
import com.apeirogon.rush.api.TestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CouponApiTest
@DisplayName("POST /coupons")
public class POST_specs {

    @Test
    void 올바르게_요청하면_200_OK_상태코드를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void 올바르게_요청하면_생성된_쿠폰_ID를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void quantity가_0이면_400_Bad_Request를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void quantity가_음수이면_400_Bad_Request를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void quantity가_없으면_400_Bad_Request를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act

        // Assert
    }
}
