package com.team.corporate.backend.services;

import com.team.corporate.backend.dto.ChangePasswordRequest;
import com.team.corporate.backend.dto.CreateUserRequest;
import com.team.corporate.backend.dto.UserResponse;
import com.team.corporate.backend.entities.UserCredentials;
import com.team.corporate.backend.repositories.SpringUsersRepository;
import com.team.corporate.backend.repositories.UserCredentialsRepository;
import com.team.corporate.core.entities.User;
import com.team.corporate.core.exceptions.UserNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AccountsService {
    private final SpringUsersRepository users;
    private final UserCredentialsRepository credentials;
    private final PasswordEncoder encoder;
    private final RefreshSessionsService sessions;

    public AccountsService(
            @NotNull SpringUsersRepository users,
            @NotNull UserCredentialsRepository credentials,
            @NotNull PasswordEncoder encoder,
            @NotNull RefreshSessionsService sessions
    ) {
        this.users = users;
        this.credentials = credentials;
        this.encoder = encoder;
        this.sessions = sessions;
    }

    @NotNull
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse createUser(@NotNull CreateUserRequest request) {
        User user = new User(request.username(), request.role());
        if (users.findByUsername(user.getUsername()).isPresent()) {
            throw new DataIntegrityViolationException("Пользователь уже существует.");
        }
        users.save(user);
        credentials.saveAndFlush(new UserCredentials(user, encoder.encode(request.password())));
        return UserResponse.of(user);
    }

    @NotNull
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return users.findAll().stream().map(UserResponse::of).toList();
    }

    @NotNull
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public UserResponse getUser(@NotNull UUID id) {
        return UserResponse.of(findUser(id));
    }

    @NotNull
    @PreAuthorize("isAuthenticated() and #username == authentication.name")
    @Transactional(readOnly = true)
    public UserResponse currentUser(@NotNull String username) {
        return users.findByUsername(username).map(UserResponse::of)
                .orElseThrow(() -> new BadCredentialsException("Учётная запись недоступна."));
    }

    @PreAuthorize("isAuthenticated() and #username == authentication.name")
    @Transactional
    public void changePassword(@NotNull String username, @NotNull ChangePasswordRequest request) {
        var user = users.findByUsernameForUpdate(username)
                .orElseThrow(() -> new BadCredentialsException("Учётная запись недоступна."));
        UserCredentials account = credentials.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Учётная запись недоступна."));
        if (!encoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new BadCredentialsException("Неверное имя пользователя или пароль.");
        }
        account.setPasswordHash(encoder.encode(request.newPassword()));
        credentials.save(account);
        sessions.revokeAll(user.getId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void resetPassword(@NotNull UUID userId, @NotNull String password) {
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден."));
        UserCredentials account = credentials.findById(userId)
                .orElseGet(() -> new UserCredentials(user, ""));
        account.setPasswordHash(encoder.encode(password));
        credentials.save(account);
        sessions.revokeAll(userId);
    }

    @NotNull
    private User findUser(@NotNull UUID id) {
        return users.findById(id).orElseThrow(() -> new UserNotFoundException("Пользователь не найден."));
    }
}
