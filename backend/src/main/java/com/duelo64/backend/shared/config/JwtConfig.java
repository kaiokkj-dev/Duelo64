package com.duelo64.backend.shared.config;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

        @Bean
        SecretKey jwtSecretKey(
                        @Value("${JWT_SECRET}") String encodedSecret) {

                byte[] secretBytes = Base64.getDecoder()
                                .decode(encodedSecret);

                return new SecretKeySpec(
                                secretBytes,
                                "HmacSHA256");
        }

        @Bean
        JwtEncoder jwtEncoder(SecretKey secretKey) {
                return NimbusJwtEncoder
                                .withSecretKey(secretKey)
                                .algorithm(MacAlgorithm.HS256)
                                .build();
        }

        @Bean
        JwtDecoder jwtDecoder(
                        SecretKey secretKey,
                        @Value("${JWT_ISSUER}") String issuer) {

                NimbusJwtDecoder decoder = NimbusJwtDecoder
                                .withSecretKey(secretKey)
                                .macAlgorithm(MacAlgorithm.HS256)
                                .build();

                decoder.setJwtValidator(
                                JwtValidators.createDefaultWithIssuer(issuer));

                return decoder;
        }
}