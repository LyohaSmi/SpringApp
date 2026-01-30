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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @GetMapping("/generate-hash")
    public ResponseEntity<?> generateHash(@RequestParam String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);

        boolean matches = encoder.matches(password, hash);

        return ResponseEntity.ok(Map.of(
                "password", password,
                "hash", hash,
                "matches", matches,
                "note", "Use this hash in your migration file"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for: {}", request.getUsernameOrEmail());

        try {
            // Находим пользователя
            User user = userRepository.findByUsername(request.getUsernameOrEmail())
                    .orElseGet(() -> userRepository.findByEmail(request.getUsernameOrEmail())
                            .orElseThrow(() -> new BadCredentialsException("Invalid credentials")));

            log.debug("User found: {}", user.getUsername());

            // Аутентифицируем пользователя через AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),  // Используем username, не email
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // Генерируем токены
            String accessToken = jwtService.generateAccessToken(userDetails, userDetails.getId());
            String refreshToken = jwtService.generateRefreshToken(userDetails, userDetails.getId());

            // Сохраняем refresh token в БД
            refreshTokenService.createRefreshToken(userDetails);

            // Возвращаем ответ
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
        // Проверяем refresh token в БД
        var refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());

        // Получаем пользователя
        User user = refreshToken.getUser();
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Генерируем новые токены
        String newAccessToken = jwtService.generateAccessToken(userDetails, userDetails.getId());
        String newRefreshToken = jwtService.generateRefreshToken(userDetails, userDetails.getId());

        // Отзываем старый refresh token
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());

        // Сохраняем новый refresh token
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

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(@RequestParam Long userId) {
        refreshTokenService.revokeAllUserRefreshTokens(userId);
        return ResponseEntity.ok("Logged out from all devices");
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

    @PostMapping("/reset-passwords")
    @Transactional
    public ResponseEntity<?> resetPasswords() {
        String newPassword = "password123";
        String newHash = passwordEncoder.encode(newPassword);

        List<User> users = userRepository.findAll();
        List<Map<String, String>> updatedUsers = new ArrayList<>();

        for (User user : users) {
            String oldHash = user.getPassword();
            user.setPassword(newHash);
            userRepository.save(user);

            updatedUsers.add(Map.of(
                    "username", user.getUsername(),
                    "oldHashPrefix", oldHash.substring(0, Math.min(30, oldHash.length())),
                    "newHashPrefix", newHash.substring(0, Math.min(30, newHash.length()))
            ));

            log.info("Updated password for user: {}", user.getUsername());
        }

        return ResponseEntity.ok(Map.of(
                "message", "Passwords reset successfully",
                "newPassword", newPassword,
                "usersUpdated", updatedUsers
        ));
    }

    @GetMapping("/debug-passwords")
    public Map<String, Object> debugPasswords() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        List<User> users = userRepository.findAll();

        Map<String, Object> result = new HashMap<>();
        Map<String, Map<String, Object>> userChecks = new HashMap<>();

        for (User user : users) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("dbHash", user.getPassword());
            userInfo.put("hashLength", user.getPassword().length());
            userInfo.put("hashStartsWith", user.getPassword().substring(0, Math.min(30, user.getPassword().length())));

            // Проверяем разные пароли
            Map<String, Boolean> passwordChecks = new HashMap<>();
            String[] testPasswords = {
                    "password123",    // Стандартный тестовый
                    "Password123!",   // Сложный вариант
                    "admin",          // Простой
                    "123456",         // Цифровой
                    "qwerty",         // Простой буквенный
                    user.getUsername() // Имя пользователя как пароль
            };

            for (String testPass : testPasswords) {
                try {
                    passwordChecks.put(testPass, encoder.matches(testPass, user.getPassword()));
                } catch (Exception e) {
                    passwordChecks.put(testPass, false);
                }
            }

            userInfo.put("passwordChecks", passwordChecks);

            // Генерируем новый хеш для сравнения
            String testPassword = "Password123!";
            String newHash = encoder.encode(testPassword);
            userInfo.put("newHashForPassword123!", newHash);
            userInfo.put("newHashMatches", encoder.matches(testPassword, newHash));

            userChecks.put(user.getUsername(), userInfo);
        }

        result.put("users", userChecks);
        result.put("totalUsers", users.size());
        result.put("passwordEncoder", encoder.getClass().getName());

        // Проверяем известный хеш
        String knownHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lOBR3q8CJ5w6O6";
        result.put("knownHashTest", Map.of(
                "hash", knownHash,
                "matchesPassword123", encoder.matches("password123", knownHash),
                "matchesPassword123!", encoder.matches("Password123!", knownHash)
        ));

        return result;
    }

    @PostMapping("/reset-all-passwords")
    public ResponseEntity<?> resetAllPasswords() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String newPassword = "SecurePass123!";  // Новый надежный пароль
        String newHash = encoder.encode(newPassword);

        List<User> users = userRepository.findAll();
        List<Map<String, String>> updatedUsers = new ArrayList<>();

        for (User user : users) {
            String oldHash = user.getPassword();
            user.setPassword(newHash);
            userRepository.save(user);

            updatedUsers.add(Map.of(
                    "username", user.getUsername(),
                    "oldHash", oldHash.substring(0, Math.min(30, oldHash.length())) + "...",
                    "newHash", newHash.substring(0, Math.min(30, newHash.length())) + "...",
                    "status", "UPDATED"
            ));

            log.info("Password reset for user: {}", user.getUsername());
        }

        return ResponseEntity.ok(Map.of(
                "message", "All passwords have been reset",
                "newPassword", newPassword,
                "note", "Use this password for all users: " + newPassword,
                "updatedUsers", updatedUsers,
                "totalUpdated", users.size()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Проверяем уникальность username
            if (userRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username already exists"));
            }

            // Проверяем уникальность email
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email already exists"));
            }

            // Создаем пользователя
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(Role.USER);

            User savedUser = userRepository.save(user);

            // Автоматически логиним пользователя после регистрации
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