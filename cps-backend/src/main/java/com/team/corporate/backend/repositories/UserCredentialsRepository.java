package com.team.corporate.backend.repositories;

import com.team.corporate.backend.entities.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserCredentialsRepository extends JpaRepository<UserCredentials, UUID> {
    @Query("SELECT c FROM UserCredentials c JOIN FETCH c.user u WHERE u.username = :username")
    Optional<UserCredentials> findByUsername(@Param("username") String username);
}
