# spring-security-auth

Spring Security 필터 체인을 직접 구성해서,  
`회원가입 -> 로그인(JWT 발급) -> 이후 요청 JWT 검증` 흐름을 학습한 프로젝트입니다.

## 기술 스택

- Java 21, Spring Boot 4
- Spring Security, Spring Data JPA, MySQL
- JJWT

## 아키텍처 흐름 (도식 기준 정리)

### 1) 회원가입 (`POST /join`)

1. `JoinController`가 요청 DTO를 받음  
2. `JoinService`가 이메일 중복 체크 후 비밀번호 암호화  
3. `UserRepository`를 통해 `UserEntity` 저장

### 2) 로그인 (버전별)

#### 2-1. 이전 버전: Form 로그인 (`POST /login`, `LoginFilter`)
1. `LoginFilter`가 `UsernamePasswordAuthenticationFilter` 기반으로 로그인 요청을 가로챔  
2. 인증 시도 후 성공하면 `JWTUtil`로 JWT를 생성해 응답 헤더 `Authorization: Bearer {token}`으로 반환

#### 2-2. 이번 버전: JSON 로그인 (`POST /api/login`, `JsonLoginFilter`)
1. `SecurityConfig`에서 `UsernamePasswordAuthenticationFilter` 자리에 `JsonLoginFilter`를 배치  
2. `JsonLoginFilter`는 `AbstractAuthenticationProcessingFilter` 기반으로 `/api/login` 요청을 처리  
3. 요청의 변환/검증 준비는 `JsonLoginAuthenticationConverter`의 `convert(HttpServletRequest)` 흐름에 위임  
4. 인증 성공 시 `JWTUtil`로 JWT를 생성하고 응답 헤더 `Authorization: Bearer {token}`으로 반환

### 3) 보호된 API 요청

1. 클라이언트가 JWT를 `Authorization` 헤더에 담아 요청  
2. `JWTFilter`가 토큰 유효성/만료 확인  
3. 토큰 클레임(`email`, `username`, `role`, `provider`)으로 인증 객체 생성  
4. `SecurityContextHolder`에 `Authentication` 저장  
5. 이후 컨트롤러는 인증된 사용자로 처리

## 핵심 클래스 역할

| 클래스 | 역할 |
|---|---|
| `SecurityConfig` | 필터 체인 구성, 경로별 인가, `STATELESS` 세션 정책 |
| `LoginFilter` | form 기반 로그인 인증 시도, 성공 시 JWT 발급 |
| `JsonLoginFilter` | JSON 로그인 요청 처리, 성공 시 JWT 발급 |
| `JsonLoginAuthenticationConverter` | JSON 요청을 인증 시도용 `Authentication`으로 변환 |
| `JWTFilter` | 모든 요청에서 JWT 검증 후 `SecurityContextHolder` 설정 |
| `JWTUtil` | JWT 생성/파싱/만료 확인 |
| `CustomUserDetailsService` | 이메일 기반 사용자 조회 (`loadUserByUsername`) |
| `CustomUserDetails` | Spring Security가 사용하는 사용자 정보 어댑터 |
| `JoinService` | 회원가입 비즈니스 로직(중복 체크, 암호화, 저장) |

## 세션 방식과 차이

- 이 프로젝트는 `SessionCreationPolicy.STATELESS`를 사용합니다.
- 서버가 로그인 상태를 세션에 저장하지 않고, 요청마다 JWT로 인증 정보를 복원합니다.
- 즉, 상태 저장 주체가 서버 세션이 아니라 클라이언트의 JWT입니다.
