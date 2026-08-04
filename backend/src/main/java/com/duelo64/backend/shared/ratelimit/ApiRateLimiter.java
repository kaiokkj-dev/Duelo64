package com.duelo64.backend.shared.ratelimit;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ApiRateLimiter {

    private final RateLimitService rateLimitService;
    private final int authRequestIpLimit;
    private final int authRequestEmailLimit;
    private final int authRequestWindowMinutes;
    private final int authResendSeconds;
    private final int authVerifyIpLimit;
    private final int authVerifyEmailLimit;
    private final int authVerifyWindowMinutes;
    private final int profileUpdateLimit;
    private final int avatarUploadLimit;
    private final int userActionWindowMinutes;

    public ApiRateLimiter(
            RateLimitService rateLimitService,
            @Value("${RATE_LIMIT_AUTH_REQUEST_IP:40}") int authRequestIpLimit,
            @Value("${RATE_LIMIT_AUTH_REQUEST_EMAIL:12}") int authRequestEmailLimit,
            @Value("${RATE_LIMIT_AUTH_REQUEST_WINDOW_MINUTES:15}") int authRequestWindowMinutes,
            @Value("${RATE_LIMIT_AUTH_RESEND_SECONDS:10}") int authResendSeconds,
            @Value("${RATE_LIMIT_AUTH_VERIFY_IP:80}") int authVerifyIpLimit,
            @Value("${RATE_LIMIT_AUTH_VERIFY_EMAIL:30}") int authVerifyEmailLimit,
            @Value("${RATE_LIMIT_AUTH_VERIFY_WINDOW_MINUTES:15}") int authVerifyWindowMinutes,
            @Value("${RATE_LIMIT_PROFILE_UPDATE:20}") int profileUpdateLimit,
            @Value("${RATE_LIMIT_AVATAR_UPLOAD:10}") int avatarUploadLimit,
            @Value("${RATE_LIMIT_USER_ACTION_WINDOW_MINUTES:60}") int userActionWindowMinutes) {

        this.rateLimitService = rateLimitService;
        this.authRequestIpLimit = authRequestIpLimit;
        this.authRequestEmailLimit = authRequestEmailLimit;
        this.authRequestWindowMinutes = authRequestWindowMinutes;
        this.authResendSeconds = authResendSeconds;
        this.authVerifyIpLimit = authVerifyIpLimit;
        this.authVerifyEmailLimit = authVerifyEmailLimit;
        this.authVerifyWindowMinutes = authVerifyWindowMinutes;
        this.profileUpdateLimit = profileUpdateLimit;
        this.avatarUploadLimit = avatarUploadLimit;
        this.userActionWindowMinutes = userActionWindowMinutes;
    }

    public void checkAuthCodeRequest(HttpServletRequest request, String email) {
        String ipAddress = clientIp(request);
        String normalizedEmail = normalizeEmail(email);
        Duration requestWindow = Duration.ofMinutes(authRequestWindowMinutes);

        rateLimitService.check(
                "auth:request:ip:" + ipAddress,
                authRequestIpLimit,
                requestWindow);

        rateLimitService.check(
                "auth:request:email:" + normalizedEmail,
                authRequestEmailLimit,
                requestWindow);

        rateLimitService.checkCooldown(
                "auth:resend:email:" + normalizedEmail,
                Duration.ofSeconds(authResendSeconds));
    }

    public void checkAuthCodeVerification(HttpServletRequest request, String email) {
        String ipAddress = clientIp(request);
        String normalizedEmail = normalizeEmail(email);
        Duration verificationWindow = Duration.ofMinutes(authVerifyWindowMinutes);

        rateLimitService.check(
                "auth:verify:ip:" + ipAddress,
                authVerifyIpLimit,
                verificationWindow);

        rateLimitService.check(
                "auth:verify:email:" + normalizedEmail,
                authVerifyEmailLimit,
                verificationWindow);
    }

    public void checkProfileUpdate(UUID userId) {
        rateLimitService.check(
                "user:profile:" + userId,
                profileUpdateLimit,
                Duration.ofMinutes(userActionWindowMinutes));
    }

    public void checkAvatarUpload(UUID userId) {
        rateLimitService.check(
                "user:avatar:" + userId,
                avatarUploadLimit,
                Duration.ofMinutes(userActionWindowMinutes));
    }

    private String clientIp(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank()
                ? "unknown"
                : remoteAddress;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
