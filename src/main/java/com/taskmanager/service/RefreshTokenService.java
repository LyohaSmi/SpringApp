package com.taskmanager.service;

import com.taskmanager.model.RefreshToken;
import com.taskmanager.model.User;
import com.taskmanager.repository.RefreshTokenRepository;
import com.taskmanager.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${application.security.jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Transactional
    public RefreshToken createRefreshToken(UserDetailsImpl userDetails) {
        User user = User.builder()
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .password(userDetails.getPassword())
                .build();

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000);

        RefreshToken refreshToken = new RefreshToken(user, token, expiresAt);
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken verifyRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(RefreshToken::isValid)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }
}