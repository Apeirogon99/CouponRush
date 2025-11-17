package com.apeirogon.rush.config;

import com.apeirogon.rush.storage.CouponRepository;
import com.apeirogon.rush.storage.IssuedCouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class SpringConfig {

    private final DataSource dataSource;


    @Autowired
    public SpringConfig(
            DataSource dataSource
    ) {
        this.dataSource = dataSource;
    }

    @Bean
    public CouponRepository couponRepository() {
        return new CouponRepository(dataSource);
    }

    @Bean
    public IssuedCouponRepository issuedCouponRepository() {
        return new IssuedCouponRepository(dataSource);
    }

}
