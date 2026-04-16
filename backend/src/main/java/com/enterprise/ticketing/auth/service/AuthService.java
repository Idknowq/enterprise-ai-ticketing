package com.enterprise.ticketing.auth.service;

import com.enterprise.ticketing.auth.dto.CurrentUserResponse;
import com.enterprise.ticketing.auth.dto.LoginRequest;
import com.enterprise.ticketing.auth.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    CurrentUserResponse currentUser();
}
