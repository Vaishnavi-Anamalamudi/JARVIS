package com.adaptivegateway.auth.repository;

import com.adaptivegateway.auth.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    @Query("""
            select user from AppUser user
            join fetch user.role
            where user.deletedAt is null
            and lower(user.username) = lower(:username)
            """)
    Optional<AppUser> findActiveByUsername(@Param("username") String username);

    @Query("""
            select user from AppUser user
            join fetch user.role
            where user.deletedAt is null
            and lower(user.email) = lower(:email)
            """)
    Optional<AppUser> findActiveByEmail(@Param("email") String email);

    @Query("""
            select user from AppUser user
            join fetch user.role
            where user.deletedAt is null
            and user.id = :id
            """)
    Optional<AppUser> findActiveById(@Param("id") UUID id);

    @Query("""
            select count(user) from AppUser user
            where user.deletedAt is null
            """)
    long countActiveUsers();

    boolean existsByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);
}
