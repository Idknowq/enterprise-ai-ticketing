package com.enterprise.ticketing.auth.bootstrap;

import com.enterprise.ticketing.auth.domain.SystemRole;
import com.enterprise.ticketing.auth.entity.RoleEntity;
import com.enterprise.ticketing.auth.entity.UserEntity;
import com.enterprise.ticketing.auth.repository.RoleRepository;
import com.enterprise.ticketing.auth.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.modules", name = "auth", havingValue = "true", matchIfMissing = true)
public class AuthDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthDataInitializer.class);
    private static final String DEFAULT_PASSWORD = "ChangeMe123!";

    private static final Map<SystemRole, SeedUser> DEFAULT_USERS = Map.of(
            SystemRole.EMPLOYEE, new SeedUser("employee01", "Employee Demo", "employee01@enterprise.local", "GENERAL"),
            SystemRole.SUPPORT_AGENT, new SeedUser("support01", "Support Demo", "support01@enterprise.local", "IT"),
            SystemRole.APPROVER, new SeedUser("approver01", "Approver Demo", "approver01@enterprise.local", "IT"),
            SystemRole.ADMIN, new SeedUser("admin01", "Admin Demo", "admin01@enterprise.local", "PLATFORM")
    );

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthDataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<SystemRole, RoleEntity> roleEntities = initializeRoles();
        DEFAULT_USERS.forEach((role, seedUser) -> initializeUser(seedUser, Set.of(roleEntities.get(role))));
        log.info("Auth seed completed. Default users: employee01/support01/approver01/admin01, password={}", DEFAULT_PASSWORD);
    }

    private Map<SystemRole, RoleEntity> initializeRoles() {
        return Map.of(
                SystemRole.EMPLOYEE, ensureRole(SystemRole.EMPLOYEE, "Employee", "Standard requester role"),
                SystemRole.SUPPORT_AGENT, ensureRole(SystemRole.SUPPORT_AGENT, "Support Agent", "First-line support role"),
                SystemRole.APPROVER, ensureRole(SystemRole.APPROVER, "Approver", "Approval decision role"),
                SystemRole.ADMIN, ensureRole(SystemRole.ADMIN, "Administrator", "Platform administration role")
        );
    }

    private RoleEntity ensureRole(SystemRole role, String name, String description) {
        return roleRepository.findByCode(role).orElseGet(() -> {
            RoleEntity roleEntity = new RoleEntity();
            roleEntity.setCode(role);
            roleEntity.setName(name);
            roleEntity.setDescription(description);
            return roleRepository.save(roleEntity);
        });
    }

    private void initializeUser(SeedUser seedUser, Set<RoleEntity> roles) {
        userRepository.findByUsernameIgnoreCase(seedUser.username()).orElseGet(() -> {
            UserEntity userEntity = new UserEntity();
            userEntity.setUsername(seedUser.username());
            userEntity.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
            userEntity.setDisplayName(seedUser.displayName());
            userEntity.setEmail(seedUser.email());
            userEntity.setDepartment(seedUser.department());
            userEntity.setEnabled(true);
            userEntity.setRoles(new LinkedHashSet<>(roles));
            return userRepository.save(userEntity);
        });
    }

    private record SeedUser(String username, String displayName, String email, String department) {
    }
}
