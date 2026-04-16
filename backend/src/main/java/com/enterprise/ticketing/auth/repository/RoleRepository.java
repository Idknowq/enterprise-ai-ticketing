package com.enterprise.ticketing.auth.repository;

import com.enterprise.ticketing.auth.domain.SystemRole;
import com.enterprise.ticketing.auth.entity.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByCode(SystemRole code);
}
