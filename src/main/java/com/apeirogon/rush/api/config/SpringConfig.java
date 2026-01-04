package com.apeirogon.rush.api.config;

import com.apeirogon.rush.storage.JdbcCouponRepository;
import com.apeirogon.rush.storage.JdbcIssuedCouponRepository;
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
    public JdbcCouponRepository couponRepository() {
        return new JdbcCouponRepository(dataSource);
    }

    @Bean
    public JdbcIssuedCouponRepository issuedCouponRepository() {
        return new JdbcIssuedCouponRepository(dataSource);
    }

}
