package com.hello.auth.dto;

import com.hello.auth.entity.UserEntity;

public record JoinDTO (
    String email,
    String password,
    String name
){
    public UserEntity toEntity(String encodedPassword) {
        return new UserEntity(
                this.email,
                this.name,
                encodedPassword,
                "ROLE_ADMIN",
                "local"
        );
    }
}