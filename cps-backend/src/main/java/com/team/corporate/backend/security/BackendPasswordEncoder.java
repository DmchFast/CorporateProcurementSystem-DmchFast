package com.team.corporate.backend.security;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BackendPasswordEncoder extends DelegatingPasswordEncoder {
    public BackendPasswordEncoder() {
        super("bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder()));
    }

    @Override
    protected boolean matchesNonNull(@NotNull String rawPassword, @NotNull String encodedPassword) {
        return rawPassword.getBytes(StandardCharsets.UTF_8).length <= 72
                && super.matchesNonNull(rawPassword, encodedPassword);
    }
}
