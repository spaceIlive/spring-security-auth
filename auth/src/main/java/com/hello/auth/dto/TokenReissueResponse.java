package com.hello.auth.dto;

public record TokenReissueResponse(
        String accessToken,
        String refreshToken
) {
}