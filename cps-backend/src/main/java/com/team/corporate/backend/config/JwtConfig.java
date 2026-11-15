package com.team.corporate.backend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({TokenProperties.class, RateLimitProperties.class})
@EnableScheduling
public class JwtConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecureRandom tokenRandom() {
        return new SecureRandom();
    }

    @Bean
    public SecretKey jwtSigningKey(@Value("${cps.security.jwt.secret:}") String encoded) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET must be Base64 containing at least 32 random bytes.");
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be Base64 containing at least 32 random bytes.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey key, TokenProperties properties, Clock clock) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestamps,
                new JwtIssuerValidator(properties.issuer()),
                new JwtClaimValidator<List<String>>("aud", aud -> aud.contains(properties.audience())),
                new JwtClaimValidator<Instant>("exp", expires -> expires.isAfter(clock.instant())),
                new JwtClaimValidator<Instant>("iat", issued -> !issued.isAfter(clock.instant())),
                new JwtClaimValidator<String>("token_use", "access"::equals)));
        return decoder;
    }
}
