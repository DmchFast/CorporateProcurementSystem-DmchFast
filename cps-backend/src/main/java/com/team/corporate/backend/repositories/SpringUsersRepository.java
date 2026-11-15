package com.team.corporate.backend.repositories;

import com.team.corporate.core.entities.User;
import com.team.corporate.core.repositories.UsersRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public interface SpringUsersRepository extends UsersRepository, ListCrudRepository<User, UUID> {
    @NotNull
    Optional<User> findByUsername(@NotNull String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(propagation = Propagation.MANDATORY)
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsernameForUpdate(@Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(propagation = Propagation.MANDATORY)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    @Override
    @Transactional
    @NotNull
    default User add(@NotNull User user) {
        return save(user);
    }

    @Override
    @NotNull
    default List<User> getAll() {
        return findAll();
    }

    @Override
    @NotNull
    default Optional<User> getById(@NotNull UUID id) {
        return findById(id);
    }

    @Override
    @Transactional
    @NotNull
    default User update(@NotNull User user) {
        return save(user);
    }

    @Override
    @Transactional
    default void delete(@NotNull UUID id) {
        deleteById(id);
    }
}
