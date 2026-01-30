package com.taskmanager.controller;

import com.taskmanager.dto.request.LoginRequest;
import com.taskmanager.dto.request.RefreshTokenRequest;
import com.taskmanager.dto.request.RegisterRequest;
import com.taskmanager.dto.response.AuthResponse;
import com.taskmanager.model.Role;
import com.taskmanager.model.User;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.JwtService;
import com.taskmanager.security.UserDetailsImpl;
import com.taskmanager.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for: {}", request.getUsernameOrEmail());

        try {
            User user = userRepository.findByUsername(request.getUsernameOrEmail())
                    .orElseGet(() -> userRepository.findByEmail(request.getUsernameOrEmail())
                            .orElseThrow(() -> new BadCredentialsException("Invalid credentials")));

            log.debug("User found: {}", user.getUsername());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            String accessToken = jwtService.generateAccessToken(userDetails, userDetails.getId());
            String refreshToken = jwtService.generateRefreshToken(userDetails, userDetails.getId());

            refreshTokenService.createRefreshToken(userDetails);

            AuthResponse response = AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpirationInSeconds())
                    .userId(userDetails.getId())
                    .username(userDetails.getUsername())
                    .role(user.getRole().name())
                    .build();

            log.info("Login successful for user: {}", user.getUsername());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Login failed for {}: {}", request.getUsernameOrEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Invalid credentials",
                            "message", "Invalid username/email or password"
                    ));
        } catch (Exception e) {
            log.error("Login error for {}: {}", request.getUsernameOrEmail(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Login failed",
                            "message", e.getMessage()
                    ));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());

        User user = refreshToken.getUser();
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        String newAccessToken = jwtService.generateAccessToken(userDetails, userDetails.getId());
        String newRefreshToken = jwtService.generateRefreshToken(userDetails, userDetails.getId());

        refreshTokenService.revokeRefreshToken(request.getRefreshToken());

        refreshTokenService.createRefreshToken(userDetails);

        AuthResponse response = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationInSeconds())
                .userId(userDetails.getId())
                .username(userDetails.getUsername())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Возвращаем информацию о текущем пользователе
        return ResponseEntity.ok(userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found")));
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            if (userRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username already exists"));
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email already exists"));
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(Role.USER);

            User savedUser = userRepository.save(user);

            UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);
            String accessToken = jwtService.generateAccessToken(userDetails, userDetails.getId());
            String refreshToken = jwtService.generateRefreshToken(userDetails, userDetails.getId());
            refreshTokenService.createRefreshToken(userDetails);

            AuthResponse response = AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpirationInSeconds())
                    .userId(userDetails.getId())
                    .username(userDetails.getUsername())
                    .role(savedUser.getRole().name())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Registration failed", "message", e.getMessage()));
        }
    }
}