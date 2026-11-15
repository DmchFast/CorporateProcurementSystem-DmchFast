package com.team.corporate.backend.repositories;

import com.team.corporate.backend.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokensRepository extends JpaRepository<RefreshToken, String> {
    @Query("SELECT t.session.id AS sessionId, t.session.user.id AS userId FROM RefreshToken t WHERE t.tokenHash = :hash")
    Optional<SessionReference> findSession(@Param("hash") String hash);

    interface SessionReference {
        UUID getSessionId();
        UUID getUserId();
    }
}
