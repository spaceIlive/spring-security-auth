package com.hello.auth.repository;

import com.hello.auth.entity.RefreshEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RefreshRepository extends JpaRepository<RefreshEntity, Long> {

    Boolean existsByRefreshToken(String refreshToken);

    void deleteByRefreshToken(String refreshToken);

}
