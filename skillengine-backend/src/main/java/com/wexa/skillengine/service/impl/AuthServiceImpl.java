package com.wexa.skillengine.service.impl;

import com.wexa.skillengine.config.JwtTokenProvider;
import com.wexa.skillengine.dto.request.LoginRequest;
import com.wexa.skillengine.dto.request.RegisterRequest;
import com.wexa.skillengine.dto.response.AuthResponse;
import com.wexa.skillengine.repository.UserRepository;
import com.wexa.skillengine.service.AuthService;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        // Public self-registration can only ever produce ROLE_USER — a caller passing
        // "role": "ROLE_ADMIN" in the request body is silently ignored. Admin accounts
        // are provisioned exclusively via DataSeeder or a trusted internal process.
        String role = "ROLE_USER";

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        Node created = userRepository.createUser(request.getEmail(), hashedPassword, role);

        String token = jwtTokenProvider.generateToken(
                created.get("email").asString(),
                created.get("role").asString()
        );

        return new AuthResponse(token, created.get("email").asString(), created.get("role").asString());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Node userNode = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        String storedHash = userNode.get("password").asString();
        if (!passwordEncoder.matches(request.getPassword(), storedHash)) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        String email = userNode.get("email").asString();
        String role = userNode.get("role").asString();
        String token = jwtTokenProvider.generateToken(email, role);

        return new AuthResponse(token, email, role);
    }
}
