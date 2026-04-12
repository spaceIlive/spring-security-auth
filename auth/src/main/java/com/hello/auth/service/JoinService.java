package com.hello.auth.service;

import com.hello.auth.dto.JoinDTO;
import com.hello.auth.entity.UserEntity;
import com.hello.auth.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class JoinService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public JoinService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder){
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public void joinProcess(JoinDTO joinDTO){
        String username = joinDTO.getUsername();
        String password = joinDTO.getPassword();

        Boolean isExist = userRepository.existsByName(username);

        //이미 존재한다.
        if(isExist){
            return;
        }
        //값 세팅
        UserEntity userEntity = new UserEntity();
        userEntity.setName(username);
        userEntity.setPassword(bCryptPasswordEncoder.encode(password));
        userEntity.setRole("ROLE_ADMIN");
        //레퍼지토리에 저장(디비에 저장)
        userRepository.save(userEntity);


    }
}
