package com.team.corporate.backend.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("cps.security.jwt")
public record TokenProperties(
        @DefaultValue("https://corporate-procurement.local") @NotBlank String issuer,
        @DefaultValue("cps-api") @NotBlank String audience,
        @DefaultValue("PT15M") Duration accessTtl,
        @DefaultValue("P30D") Duration refreshTtl
) {
    public TokenProperties {
        if (accessTtl == null || accessTtl.isNegative() || accessTtl.isZero()
                || refreshTtl == null || refreshTtl.compareTo(accessTtl) <= 0) {
            throw new IllegalArgumentException("Refresh lifetime must exceed a positive access-token lifetime.");
        }
    }
}
