package com.enterprise.ticketing.auth.repository;

import com.enterprise.ticketing.auth.entity.UserEntity;
import com.enterprise.ticketing.auth.domain.SystemRole;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "roles")
    @Query("""
            select distinct u
            from UserEntity u
            join u.roles r
            where u.enabled = true and r.code = :role
            order by u.id asc
            """)
    List<UserEntity> findEnabledUsersByRole(SystemRole role);
}
