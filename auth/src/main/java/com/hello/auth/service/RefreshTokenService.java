package com.hello.auth.service;

import com.hello.auth.dto.TokenReissueResponse;
import com.hello.auth.entity.RefreshEntity;
import com.hello.auth.repository.RefreshRepository;
import com.hello.auth.utils.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class RefreshTokenService {

    private final RefreshRepository refreshRepository;
    private final JWTUtil jwtUtil;

    public RefreshTokenService(RefreshRepository refreshRepository, JWTUtil jwtUtil) {
        this.refreshRepository = refreshRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public TokenReissueResponse reissue(String refreshToken) {
        //검증
        validateRefreshToken(refreshToken);

        String email = jwtUtil.getEmail(refreshToken);
        String role = jwtUtil.getRole(refreshToken);
        String name = jwtUtil.getUsername(refreshToken);
        String provider = jwtUtil.getProvider(refreshToken);

        String newAccessToken = jwtUtil.createJwt(
                "access",
                email,
                role,
                name,
                provider,
                60 * 60 * 1000L
        );

        String newRefreshToken = jwtUtil.createJwt(
                "refresh",
                email,
                role,
                name,
                provider,
                60 * 60 * 24 * 30 * 1000L
        );

        deleteByRefreshToken(refreshToken);
        save(email, newRefreshToken, 60 * 60 * 24 * 30 * 1000L);

        return new TokenReissueResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void save(String email, String refreshToken, Long expiration) {
        Date date = new Date(System.currentTimeMillis() + expiration);
        RefreshEntity refreshEntity = new RefreshEntity(email, refreshToken, date.toString());
        refreshRepository.save(refreshEntity);
    }

    @Transactional
    public void deleteByRefreshToken(String refreshToken) {
        refreshRepository.deleteByRefreshToken(refreshToken);
    }

    private void validateRefreshToken(String refreshToken) {
        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException("refresh token expired");
        }

        String category = jwtUtil.getCategory(refreshToken);
        if (!"refresh".equals(category)) {
            throw new IllegalArgumentException("invalid refresh token");
        }

        Boolean isExist = refreshRepository.existsByRefreshToken(refreshToken);
        if (!Boolean.TRUE.equals(isExist)) {
            throw new IllegalArgumentException("refresh token not found");
        }
    }
}