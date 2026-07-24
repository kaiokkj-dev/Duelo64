package com.duelo64.backend.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.resend.Resend;

@Configuration
public class ResendConfig {

    @Bean
    Resend resend(@Value("${RESEND_API_KEY}") String apiKey) {
        return new Resend(apiKey);
    }
}