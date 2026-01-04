package com.apeirogon.rush.api.issue;

import com.apeirogon.rush.api.CouponApiTest;
import com.apeirogon.rush.api.TestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CouponApiTest
@DisplayName("POST /coupons/{couponId}/issues")
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
    void 발급_후_쿠폰의_issuedQuantity가_1_증가한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void 존재하지_않는_쿠폰이면_404_Not_Found를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void 동일_사용자가_중복_발급하면_409_Conflict를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void 재고가_소진되면_410_Gone을_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act

        // Assert
    }

    @Test
    void userId가_없으면_400_Bad_Request를_반환한다(
            @Autowired TestFixture fixture
    ) {
        // Arrange

        // Act

        // Assert
    }
}
