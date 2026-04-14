package com.hello.auth.dto;

import com.hello.auth.entity.UserEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private final UserEntity userEntity;

    public CustomUserDetails (UserEntity userEntity){
        this.userEntity = userEntity;
    }

    //role 값 반환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();
        collection.add(new GrantedAuthority() {
            @Override
            public @Nullable String getAuthority() {
                return userEntity.getRole();
            }
        });
        return collection;
    }

    @Override
    public @Nullable String getPassword() {
        return userEntity.getPassword();
    }
    //우리는 식별자가 이메일이랑 같아서
    @Override
    public String getUsername() {
        return userEntity.getEmail();
    }
    //이게 유저이름 가져오는거
    public String getName() {
        return userEntity.getName();
    }

    public String getProvider() {
        return userEntity.getProvider();
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
