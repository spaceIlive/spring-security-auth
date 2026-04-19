package com.hello.auth.utils;

import com.hello.auth.dto.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collection;
import java.util.Iterator;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        //원래 디폴트가 "username"이라서 요청에서 뽑을 단어 "email"로 바꿔줘야함 이거 밑에서 obtainUsername(request)할때 email을 기준으로 파싱
        setUsernameParameter("email");
        // 기본값은 /login 이지만, 여기서 생성 시점에 아래 메서드 이용해서 바꿀 수 있다
        //setFilterProcessesUrl("/api/v1/auth/login");
    }


    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException{

        //클라이언트 요청에서 email, password 추출(form data 에서만 가능)
        //json은 파싱기능을 따로 구현해야함(이거할때는 컨트롤러단에서 로그인 처리가 더 편할수도)
        String email = obtainUsername(request);
        String password = obtainPassword(request);

        //스프링 시큐리티에서 email과 password를 검증하기 위해서는 token에 담아야 함
        //UsernamePasswordAuthenticationToken 는 AbstractAuthenticationToken 을 상속받고,
        //AbstractAuthenticationToken는 Authentication를 구현한것이다. 그래서 UsernamePasswordAuthenticationToken라고 해도되고 Authentication이라 해도된다.
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password, null);

        //token에 담은 검증을 위한 AuthenticationManager로 전달
        return authenticationManager.authenticate(authToken);
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

        // refresh 때문에 카테고리 추가돼서 일단 해당부분 주석처리 해둠(그리고 앞으로 폼로그인 구현할때 다르게 다시 쓸듯)
        //String token = jwtUtil.createJwt(email, role, username, provider,60*60*100L);

        //response.addHeader("Authorization", "Bearer " + token);

    }

    //로그인 실패시 실행하는 메소드
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        response.setStatus(401);
    }

}
