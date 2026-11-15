package com.team.corporate.backend.security;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SessionRevocation {
    private final SessionRegistry sessions;

    public SessionRevocation(@NotNull SessionRegistry sessions) {
        this.sessions = sessions;
    }

    @TransactionalEventListener
    public void passwordChanged(@NotNull PasswordChanged event) {
        for (Object principal : sessions.getAllPrincipals()) {
            if (principal instanceof UserDetails user && user.getUsername().equals(event.username())) {
                sessions.getAllSessions(principal, false).forEach(SessionInformation::expireNow);
            }
        }
    }

    public record PasswordChanged(@NotNull String username) {
    }
}
