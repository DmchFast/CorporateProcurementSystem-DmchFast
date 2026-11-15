package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.ChangePasswordRequest;
import com.team.corporate.backend.dto.UserResponse;
import com.team.corporate.backend.services.AccountsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AccountsService accounts;

    public AuthenticationController(AccountsService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        return accounts.currentUser(authentication.getName());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        accounts.changePassword(authentication.getName(), request);
    }
}
