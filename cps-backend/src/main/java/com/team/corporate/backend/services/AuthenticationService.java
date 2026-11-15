package com.team.corporate.backend.services;

import com.team.corporate.backend.dto.*;
import com.team.corporate.backend.repositories.SpringUsersRepository;
import com.team.corporate.backend.security.AuthenticationRateLimiter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AuthenticationService {
    private final AuthenticationManager authentication;
    private final SpringUsersRepository users;
    private final RefreshSessionsService sessions;
    private final AccountsService accounts;
    private final AuthenticationRateLimiter limiter;
    private final TransactionTemplate transactions;

    public AuthenticationService(AuthenticationManager authentication, SpringUsersRepository users,
                                 RefreshSessionsService sessions, AccountsService accounts, AuthenticationRateLimiter limiter,
                                 PlatformTransactionManager transactionManager) {
        this.authentication = authentication;
        this.users = users;
        this.sessions = sessions;
        this.accounts = accounts;
        this.limiter = limiter;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public TokenResponse login(LoginRequest request, String ip) {
        String username = request.username().strip();
        limiter.acquireAccount("login", username, ip);
        TokenResponse response = transactions.execute(status -> {
            var user = users.findByUsernameForUpdate(username).orElse(null);
            authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, request.password()));
            if (user == null) throw new BadCredentialsException("Неверные учётные данные.");
            return sessions.open(user);
        });
        limiter.accountSucceeded("login", username);
        return response;
    }

    public TokenResponse refresh(RefreshTokenRequest request, String ip) {
        limiter.acquireTokenRequest("refresh", ip);
        return sessions.refresh(request.refreshToken());
    }

    public void logout(RefreshTokenRequest request, String ip) {
        limiter.acquireTokenRequest("logout", ip);
        sessions.logout(request.refreshToken());
    }

    public void changePassword(String username, ChangePasswordRequest request, String ip) {
        limiter.acquireAccount("password-change", username, ip);
        accounts.changePassword(username, request);
        limiter.accountSucceeded("password-change", username);
    }
}
