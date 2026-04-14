package com.hello.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = true) // 소셜 가입자를 위해 null 허용
    private String password;

    private String name;
    private String role;

    // 어느 플랫폼 출신인지 저장 (예: "google", "kakao", "local")
    private String provider;


    // 생성자: 소셜 가입과 일반 가입 모두 대응 가능하도록 작성
    public UserEntity(String email, String name, String password, String role, String provider) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.role = role;
        this.provider = provider;
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateName(String newName) {
        this.name = newName;
    }
}