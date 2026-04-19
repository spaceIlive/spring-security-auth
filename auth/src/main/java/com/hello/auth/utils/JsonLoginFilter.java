package com.hello.auth.utils;

import com.hello.auth.dto.CustomUserDetails;
import com.hello.auth.service.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.util.Collection;
import java.util.Iterator;


public class JsonLoginFilter extends AbstractAuthenticationProcessingFilter {
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public JsonLoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, RefreshTokenService refreshTokenService) {
        super("/api/login", authenticationManager);
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        setAuthenticationConverter(new JsonLoginAuthenticationConverter());
    }

    //로그인 성공시 실행하는 메소드 (여기서 JWT를 발급하면 됨)
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) {
        System.out.println("로그인 성공");
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        //
        String email = customUserDetails.getUsername();
        String username = customUserDetails.getName();
        String provider = customUserDetails.getProvider();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();

        String role = auth.getAuthority();
        String accessToken = jwtUtil.createJwt("access",email, role, username, provider,60*60*1000L);
        String refreshToken = jwtUtil.createJwt("refresh",email, role, username, provider,60*60*24*30*1000L);

        //발급과 동시에 db에 리프래시토큰 저장
        refreshTokenService.save(email, refreshToken, 60*60*24*30*1000L);

        //헤더에 어세스토큰 추가
        response.addHeader("Authorization", "Bearer " + accessToken);
        // 쿠키에 리프레시 추가
        response.addCookie(createCookie("refresh", refreshToken));
        response.setStatus(HttpStatus.OK.value());

    }

    //로그인 실패시 실행하는 메소드
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        response.setStatus(401);
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        //30일 만료
        cookie.setMaxAge(60 * 60 * 24 * 30);
        // https통신만 허용
        //cookie.setSecure(true);
        //쿠키 허용 범위
        cookie.setPath("/api/reissue");
        //자바스크립트로 접근 막기
        cookie.setHttpOnly(true);

        return cookie;
    }

}
