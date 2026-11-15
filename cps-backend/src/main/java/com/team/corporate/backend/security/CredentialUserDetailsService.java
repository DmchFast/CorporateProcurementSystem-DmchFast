package com.team.corporate.backend.security;

import com.team.corporate.backend.entities.UserCredentials;
import com.team.corporate.backend.repositories.UserCredentialsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CredentialUserDetailsService implements UserDetailsService {
    private final UserCredentialsRepository credentials;

    public CredentialUserDetailsService(@NotNull UserCredentialsRepository credentials) {
        this.credentials = credentials;
    }

    private static UserDetails details(UserCredentials credentials) {
        var user = credentials.getUser();
        return User.withUsername(user.getUsername())
                .password(credentials.getPasswordHash())
                .roles(user.getUserRole().name())
                .disabled(!user.isEnabled())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @NotNull
    public UserDetails loadUserByUsername(@NotNull String username) {
        return details(findCredentials(username));
    }

    private UserCredentials findCredentials(String username) {
        return credentials.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Неверное имя пользователя или пароль."));
    }
}
