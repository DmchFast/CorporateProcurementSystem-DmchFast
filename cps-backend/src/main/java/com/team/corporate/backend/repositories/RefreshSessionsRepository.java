package com.team.corporate.backend.repositories;

import com.team.corporate.backend.entities.RefreshSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Transactional(readOnly = true)
public interface RefreshSessionsRepository extends JpaRepository<RefreshSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(propagation = Propagation.MANDATORY)
    @Query("SELECT s FROM RefreshSession s WHERE s.id = :id")
    Optional<RefreshSession> findForUpdate(@Param("id") UUID id);

    @Query("SELECT s FROM RefreshSession s JOIN FETCH s.user WHERE s.id = :id")
    Optional<RefreshSession> findWithUser(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE RefreshSession s SET s.revokedAt = :now WHERE s.user.id = :userId AND s.revokedAt IS NULL")
    void revokeForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM RefreshSession s WHERE s.expiresAt <= :now")
    void deleteExpired(@Param("now") Instant now);
}
