package com.team.corporate.backend.security;

import com.team.corporate.backend.repositories.RefreshSessionsRepository;
import com.team.corporate.backend.repositories.UserCredentialsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

@Component
public class SessionJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final RefreshSessionsRepository sessions;
    private final UserCredentialsRepository credentials;
    private final Clock clock;

    public SessionJwtAuthenticationConverter(RefreshSessionsRepository sessions,
                                             UserCredentialsRepository credentials, Clock clock) {
        this.sessions = sessions;
        this.credentials = credentials;
        this.clock = clock;
    }

    @Override
    public AbstractAuthenticationToken convert(@NotNull Jwt jwt) {
        UUID sessionId;
        UUID userId;
        try {
            sessionId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("sid")));
            userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidBearerTokenException("Недействительный токен.");
        }
        try {
            var session = sessions.findWithUser(sessionId)
                    .orElseThrow(() -> new InvalidBearerTokenException("Сессия недействительна."));
            var user = session.getUser();
            if (!session.isActive(clock.instant()) || !user.getId().equals(userId) || !credentials.existsById(userId)) {
                throw new InvalidBearerTokenException("Сессия недействительна.");
            }
            return new JwtAuthenticationToken(jwt,
                    AuthorityUtils.createAuthorityList("ROLE_" + user.getUserRole().name()), user.getUsername());
        } catch (DataAccessException | TransactionException exception) {
            throw new AuthenticationServiceException("Не удалось проверить сессию.", exception);
        }
    }
}
