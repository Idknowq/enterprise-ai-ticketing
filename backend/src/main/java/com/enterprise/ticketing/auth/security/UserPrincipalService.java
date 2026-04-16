package com.enterprise.ticketing.auth.security;

import com.enterprise.ticketing.auth.entity.UserEntity;
import com.enterprise.ticketing.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPrincipalService implements UserDetailsService {

    private final UserRepository userRepository;

    public UserPrincipalService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return loadPrincipalByUsername(username);
    }

    @Transactional(readOnly = true)
    public UserPrincipal loadPrincipalByUsername(String username) {
        UserEntity userEntity = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return UserPrincipal.from(userEntity);
    }
}
