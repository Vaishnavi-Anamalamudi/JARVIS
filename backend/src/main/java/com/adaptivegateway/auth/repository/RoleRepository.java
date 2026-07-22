package com.adaptivegateway.auth.repository;

import com.adaptivegateway.auth.entity.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
