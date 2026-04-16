package com.enterprise.ticketing.auth.security;

import com.enterprise.ticketing.auth.domain.SystemRole;
import com.enterprise.ticketing.auth.entity.UserEntity;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String displayName;
    private final String department;
    private final boolean enabled;
    private final Set<SystemRole> roles;
    private final List<GrantedAuthority> authorities;

    private UserPrincipal(
            Long id,
            String username,
            String password,
            String displayName,
            String department,
            boolean enabled,
            Set<SystemRole> roles
    ) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.department = department;
        this.enabled = enabled;
        this.roles = Set.copyOf(roles);
        this.authorities = this.roles.stream()
                .map(SystemRole::authority)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    public static UserPrincipal from(UserEntity userEntity) {
        Set<SystemRole> roles = userEntity.getRoles().stream()
                .map(RoleEntity -> RoleEntity.getCode())
                .collect(Collectors.toSet());

        return new UserPrincipal(
                userEntity.getId(),
                userEntity.getUsername(),
                userEntity.getPasswordHash(),
                userEntity.getDisplayName(),
                userEntity.getDepartment(),
                userEntity.isEnabled(),
                roles
        );
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDepartment() {
        return department;
    }

    public Set<SystemRole> getRoles() {
        return roles;
    }

    public List<String> roleNames() {
        return roles.stream()
                .map(Enum::name)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
