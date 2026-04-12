# spring-security-auth

Spring Security의 **필터 체인**과 **인증 객체(`Authentication`) 흐름**을 공부하면서, 기본 폼 로그인 대신 **커스텀 필터 + JWT**로 로그인·이후 요청 인증을 구현한 저장소입니다.

## 기술 스택

- Java 21, Spring Boot 4, Spring Security, Spring Data JPA, MySQL
- JWT(JJWT), WAR 패키징 가능

## 무엇을 공부했고, 어떻게 구현했는지

- **필터 체인**: `SecurityFilterChain`에 어떤 순서로 필터를 넣는지(`addFilterBefore`, `addFilterAt`)에 따라 요청이 거치는 경로가 달라집니다.
- **`UsernamePasswordAuthenticationFilter` 계열**: 아이디·비밀번호를 꺼내 `AuthenticationManager`에게 넘기고, 성공/실패 시점을 오버라이드하는 패턴을 `LoginFilter`로 연습했습니다.
- **`UserDetails` / `UserDetailsService`**: DB의 `UserEntity`를 `CustomUserDetails`로 감싸고, `CustomUserDetailsService`에서 `loadUserByUsername`으로 조회해 스프링 시큐리티가 비밀번호 검증을 하도록 맞췄습니다.
- **JWT**: 로그인 **한 번** 검증이 끝난 뒤에는, 서버가 세션 ID로 사용자를 찾는 대신 **클라이언트가 들고 다니는 토큰**으로 “누구인지·역할은 무엇인지”를 전달합니다. 발급·파싱은 `JWTUtil`, 이후 요청에서 읽는 일은 `JWTFilter`에 두었습니다.

## 필터 방식에서 왜 “세션 로그인”이 아니라 JWT를 썼는지

전통적인 **세션 방식**(스프링 시큐리티 기본 폼 로그인 + 서버 세션)은 대략 다음과 같습니다.

- 로그인에 성공하면 **서버(또는 세션 스토어)**에 “이 세션 ID = 이 사용자” 같은 **상태**를 남깁니다.
- 이후 요청은 **쿠키의 세션 ID**만 보내고, 서버가 그 ID로 저장소에서 사용자를 꺼냅니다.

이 프로젝트에서는 `SecurityConfig`에서 **`SessionCreationPolicy.STATELESS`**를 쓰도록 맞춰 두었습니다. 즉, **“로그인 상태를 서버 세션에 쌓아 두는 방식”을 쓰지 않겠다**는 설정 방향입니다. 그 대신 로그인 성공 시 `LoginFilter`에서 **JWT를 만들어 응답 헤더**(`Authorization: Bearer …`)로 내려 주고, 이후 API 호출마다 클라이언트가 그 토큰을 헤더에 실어내도록 했습니다.

정리하면, **“필터 = 반드시 JWT”는 아니고**, 여기서는 **필터로 로그인 흐름을 직접 제어**하면서 **상태 저장소를 세션이 아니라 토큰(클라이언트 보관)**에 두는 조합을 연습한 것입니다.

## 세션 방식이랑 뭐가 다른지

| 구분 | 세션 중심(전통 폼 로그인) | 이 프로젝트(JWT + STATELESS 방향) |
|------|---------------------------|-------------------------------------|
| 로그인 후 “무엇을 기억하나” | 서버(또는 Redis 등)의 **세션 저장소** | 서버는 **토큰 문자열만 검증**하고, 클라이언트가 토큰을 보관 |
| 이후 요청이 싣는 정보 | 보통 **세션 쿠키(JSESSIONID 등)** | **`Authorization` 헤더의 Bearer JWT** |
| 인증이 끝난 뒤 필터가 하는 일 | 세션에서 이미 올라와 있는 `SecurityContext`를 쓰는 흐름이 흔함 | `JWTFilter`가 매 요청마다 토큰을 읽고 **`SecurityContextHolder`에 `Authentication`을 새로 세팅** |

## “JWT인데도 세션 얘기가 나오는” 부분 — `SecurityContext`와 HTTP 세션은 다르다

코드 주석에 “세션에 사용자 등록”처럼 적혀 있을 수 있지만, 실제로는 **`HttpSession`에 사용자를 넣는 것**과 **`SecurityContextHolder`에 `Authentication`을 넣는 것**이 다릅니다.

- **`SecurityContextHolder`**: 보통 **요청 스레드(요청 한 번) 안**에서 “지금 이 요청은 누구로 인증되었는지”를 들고 가는 저장소에 가깝습니다. `JWTFilter`에서 토큰을 검증한 뒤 `setAuthentication`을 호출하는 것은 **이번 HTTP 요청 처리 동안** 시큐리티가 `authenticated()` / `hasRole()` 판단을 할 수 있게 만드는 단계입니다.
- **`SessionCreationPolicy.STATELESS`**: **HTTP 세션에 로그인 상태를 만들지 않거나 최소화**하는 쪽의 설정입니다. 그래도 **요청마다** `SecurityContext`를 채우는 것은 JWT 방식에서도 일반적입니다.

즉, **“JWT = SecurityContext를 아예 안 쓴다”가 아니라**, “**서버가 HTTP 세션으로 로그인 상태를 유지하지 않고**, 매 요청마다 토큰으로 `Authentication`을 **다시 구성**한다”에 가깝습니다. 그 점이 **세션 쿠키로 서버가 사용자를 기억하는 방식**과의 차이입니다.

## 커스텀한 부분 위주 정리

| 클래스 | 역할 |
|--------|------|
| `SecurityConfig` | 폼 로그인·HTTP Basic 끄기, 경로별 인가, **`JWTFilter`를 `LoginFilter` 앞에** 배치, **`LoginFilter`를 `UsernamePasswordAuthenticationFilter` 자리에** 배치, `STATELESS` |
| `LoginFilter` | `UsernamePasswordAuthenticationFilter` 상속 — `attemptAuthentication`에서 ID/PW를 꺼내 `AuthenticationManager`로 검증, **`successfulAuthentication`에서 JWT 발급 후 응답 헤더에 실어 보냄** |
| `JWTFilter` | `OncePerRequestFilter` — 요청마다 `Authorization` 헤더의 JWT를 검사·만료 확인 후, 클레임으로 `CustomUserDetails`를 만들어 **`SecurityContextHolder`에 인증 설정** |
| `JWTUtil` | 비밀키로 서명·검증, 클레임(`username`, `role`) 읽기, 만료 확인, 토큰 생성 |
| `CustomUserDetailsService` / `CustomUserDetails` | DB `UserEntity` 기반으로 `UserDetails`를 제공해 **로그인 시 비밀번호 검증**이 스프링 시큐리티 규칙대로 이뤄지게 함 |

## 실행 시 참고

- 앱 실행·DB 설정은 `auth` 모듈의 `application.properties`를 로컬 환경에 맞게 수정하면 됩니다.
- JWT 비밀키·DB 비밀번호는 **공개 저장소에는 올리지 않는 것**이 좋습니다.
