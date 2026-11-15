package com.team.corporate.backend.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("cps.security.rate-limit")
public record RateLimitProperties(
        @DefaultValue("5") @Min(1) int accountAttempts,
        @DefaultValue("PT15M") Duration accountWindow,
        @DefaultValue("30") @Min(1) int ipAttempts,
        @DefaultValue("PT1M") Duration ipWindow,
        @DefaultValue("60") @Min(1) int refreshAttempts,
        @DefaultValue("10000") @Min(10) @Max(1000000) int maxTrackedKeys
) {
    public RateLimitProperties {
        if (accountWindow == null || accountWindow.isNegative() || accountWindow.isZero()
                || ipWindow == null || ipWindow.isNegative() || ipWindow.isZero()) {
            throw new IllegalArgumentException("Rate-limit windows must be positive.");
        }
    }
}
