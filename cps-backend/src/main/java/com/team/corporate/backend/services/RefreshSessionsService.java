package com.team.corporate.backend.services;

import com.team.corporate.backend.config.TokenProperties;
import com.team.corporate.backend.dto.TokenResponse;
import com.team.corporate.backend.entities.RefreshSession;
import com.team.corporate.backend.entities.RefreshToken;
import com.team.corporate.backend.exceptions.InvalidRefreshSessionException;
import com.team.corporate.backend.repositories.*;
import com.team.corporate.core.entities.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshSessionsService {
    private final RefreshSessionsRepository sessions;
    private final RefreshTokensRepository tokens;
    private final SpringUsersRepository users;
    private final UserCredentialsRepository credentials;
    private final JwtEncoder encoder;
    private final TokenProperties properties;
    private final Clock clock;
    private final SecureRandom random;

    public RefreshSessionsService(RefreshSessionsRepository sessions, RefreshTokensRepository tokens,
                                  SpringUsersRepository users, UserCredentialsRepository credentials,
                                  JwtEncoder encoder, TokenProperties properties, Clock clock, SecureRandom random) {
        this.sessions = sessions;
        this.tokens = tokens;
        this.users = users;
        this.credentials = credentials;
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
        this.random = random;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public TokenResponse open(User user) {
        if (!user.isEnabled()) throw new DisabledException("Учётная запись отключена.");
        Instant now = clock.instant();
        RefreshSession session = sessions.saveAndFlush(new RefreshSession(user, now, now.plus(properties.refreshTtl())));
        return issue(session, user, now);
    }

    @Transactional(noRollbackFor = InvalidRefreshSessionException.class)
    public TokenResponse refresh(String rawToken) {
        String hash = hash(rawToken);
        var reference = tokens.findSession(hash).orElseThrow(InvalidRefreshSessionException::new);
        User user = users.findByIdForUpdate(reference.getUserId()).orElseThrow(InvalidRefreshSessionException::new);
        RefreshSession session = sessions.findForUpdate(reference.getSessionId()).orElseThrow(InvalidRefreshSessionException::new);
        RefreshToken token = tokens.findById(hash).orElseThrow(InvalidRefreshSessionException::new);
        Instant now = clock.instant();
        if (!session.isActive(now) || !user.isEnabled() || !credentials.existsById(user.getId())) {
            throw new InvalidRefreshSessionException();
        }
        if (token.getUsedAt() != null) {
            session.revoke(now);
            throw new InvalidRefreshSessionException();
        }
        token.markUsed(now);
        return issue(session, user, now);
    }

    @Transactional
    public void logout(String rawToken) {
        var reference = tokens.findSession(hash(rawToken)).orElse(null);
        if (reference == null || users.findByIdForUpdate(reference.getUserId()).isEmpty()) return;
        sessions.findForUpdate(reference.getSessionId()).ifPresent(session -> session.revoke(clock.instant()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeAll(UUID userId) {
        sessions.revokeForUser(userId, clock.instant());
    }

    @Scheduled(fixedDelayString = "${cps.security.sessions.cleanup-interval:PT1H}")
    @Transactional
    public void deleteExpiredSessions() {
        sessions.deleteExpired(clock.instant());
    }

    private TokenResponse issue(RefreshSession session, User user, Instant now) {
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        tokens.save(new RefreshToken(hash(refreshToken), session));
        Instant expires = now.plus(properties.accessTtl());
        if (expires.isAfter(session.getExpiresAt())) expires = session.getExpiresAt();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer()).audience(List.of(properties.audience()))
                .subject(user.getId().toString()).id(UUID.randomUUID().toString())
                .issuedAt(now).expiresAt(expires)
                .claim("sid", session.getId().toString()).claim("token_use", "access")
                .claim("roles", List.of(user.getUserRole().name())).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String accessToken = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(accessToken, refreshToken, "Bearer",
                Duration.between(now, expires).toSeconds(), Duration.between(now, session.getExpiresAt()).toSeconds());
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
