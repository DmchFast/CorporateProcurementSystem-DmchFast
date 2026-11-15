package com.team.corporate.backend.exceptions;

import org.springframework.security.authentication.BadCredentialsException;

public class InvalidRefreshSessionException extends BadCredentialsException {
    public InvalidRefreshSessionException() {
        super("Недействительная сессия. Войдите повторно.");
    }
}
