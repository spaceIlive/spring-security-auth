package com.hello.auth.utils;

import com.hello.auth.dto.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.util.Collection;
import java.util.Iterator;


public class JsonLoginFilter extends AbstractAuthenticationProcessingFilter {
    private final JWTUtil jwtUtil;

    public JsonLoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil) {
        super("/api/login", authenticationManager);
        this.jwtUtil = jwtUtil;
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
        String token = jwtUtil.createJwt(email, role, username, provider,60*60*100L);

        response.addHeader("Authorization", "Bearer " + token);

    }

    //로그인 실패시 실행하는 메소드
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        response.setStatus(401);
    }


}
