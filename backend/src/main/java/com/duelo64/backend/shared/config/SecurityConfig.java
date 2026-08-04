package com.duelo64.backend.shared.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                return http
                                .csrf(csrf -> csrf.disable())
                                .cors(Customizer.withDefaults())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers(
                                                "/api/v1/status",
                                                                "/actuator/health",
                                                                "/ws",
                                                                "/ws/**")
                                                .permitAll()
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/v1/auth/codes",
                                                                "/api/v1/auth/codes/verify")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .build();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource(
                        @Value("${ALLOWED_ORIGINS:http://127.0.0.1:5500,http://localhost:5500}") String allowedOrigins) {

                List<String> origins = Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim)
                                .filter(origin -> !origin.isBlank())
                                .toList();

                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(origins);
                configuration.setAllowedMethods(List.of(
                                "GET",
                                "POST",
                                "PATCH",
                                "OPTIONS"));
                configuration.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type"));
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", configuration);

                return source;
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
