package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.CreateUserRequest;
import com.team.corporate.backend.dto.ResetPasswordRequest;
import com.team.corporate.backend.dto.UserResponse;
import com.team.corporate.backend.services.AccountsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final AccountsService accounts;

    public UsersController(AccountsService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public List<UserResponse> getUsers() {
        return accounts.getUsers();
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable("id") UUID id) {
        return accounts.getUser(id);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = accounts.createUser(request);
        return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(user);
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable("id") UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        accounts.resetPassword(id, request.password());
    }
}
