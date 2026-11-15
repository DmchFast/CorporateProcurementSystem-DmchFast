package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.ChangePasswordRequest;
import com.team.corporate.backend.dto.LoginRequest;
import com.team.corporate.backend.dto.RefreshTokenRequest;
import com.team.corporate.backend.dto.TokenResponse;
import com.team.corporate.backend.dto.UserResponse;
import com.team.corporate.backend.services.AuthenticationService;
import com.team.corporate.backend.services.AccountsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AccountsService accounts;
    private final AuthenticationService authenticationService;

    public AuthenticationController(AccountsService accounts, AuthenticationService authenticationService) {
        this.accounts = accounts;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authenticationService.login(request, http.getRemoteAddr());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        return authenticationService.refresh(request, http.getRemoteAddr());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        authenticationService.logout(request, http.getRemoteAddr());
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        return accounts.currentUser(authentication.getName());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest http
    ) {
        authenticationService.changePassword(authentication.getName(), request, http.getRemoteAddr());
    }
}
