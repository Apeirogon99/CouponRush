package com.apeirogon.rush.api;

import com.apeirogon.rush.api.controller.request.CreateCouponRequest;
import com.apeirogon.rush.api.controller.request.IssueCouponRequest;
import com.apeirogon.rush.api.controller.response.CreateCouponResponse;
import com.apeirogon.rush.support.response.ApiResult;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

public record TestFixture(
        TestRestTemplate client
) {

    public <T> ApiResult<T> get(String url, ParameterizedTypeReference<ApiResult<T>> type) {
        ResponseEntity<ApiResult<T>> response = client.exchange(
                RequestEntity.get(url).build(),
                type
        );
        return response.getBody();
    }

    public <T> ApiResult<T> post(String url, Object body, ParameterizedTypeReference<ApiResult<T>> type) {
        ResponseEntity<ApiResult<T>> response = client.exchange(
                RequestEntity.post(url).body(body),
                type
        );
        return response.getBody();
    }

    public Long createCoupon() {
        return createCoupon(100);
    }

    public Long createCoupon(int quantity) {
        ApiResult<CreateCouponResponse> result = post(
                "/coupons",
                new CreateCouponRequest(quantity),
                new ParameterizedTypeReference<>() { }
        );
        return Objects.requireNonNull(result.getData()).id();
    }

    public void issueCoupon(Long couponId, Long userId) {
        post(
                "/coupons/" + couponId + "/issues",
                new IssueCouponRequest(userId, couponId),
                new ParameterizedTypeReference<>() { }
        );
    }
}
