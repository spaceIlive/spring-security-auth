package com.hello.auth.utils;

import com.hello.auth.dto.LoginDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonLoginAuthenticationConverter implements AuthenticationConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public @Nullable Authentication convert(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        String contentType = request.getContentType();
        if (contentType == null || !contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            throw new AuthenticationServiceException("Content-Type은 application/json 이어야 합니다.");
        }

        try {
            LoginDTO loginDTO = objectMapper.readValue(request.getInputStream(), LoginDTO.class);

            String email = loginDTO.email();
            String password = loginDTO.password();

            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                throw new AuthenticationServiceException("email 또는 password가 비어 있습니다.");
            }

            return UsernamePasswordAuthenticationToken.unauthenticated(email, password);
        } catch (IOException e) {
            throw new AuthenticationServiceException("JSON 로그인 요청을 읽는 데 실패했습니다.", e);
        }
    }
}
