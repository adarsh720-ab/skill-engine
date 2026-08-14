package com.wexa.skillengine.service;

import com.wexa.skillengine.dto.request.LoginRequest;
import com.wexa.skillengine.dto.request.RegisterRequest;
import com.wexa.skillengine.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
