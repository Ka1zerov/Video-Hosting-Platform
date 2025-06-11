package com.tskrypko.authentication.service;

import com.tskrypko.authentication.dto.RegistrationRequest;
import com.tskrypko.authentication.exception.UserAlreadyExistsException;
import com.tskrypko.authentication.mapper.UserMapper;
import com.tskrypko.authentication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegistrationRequest registrationRequest) {

        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            throw new UserAlreadyExistsException();
        }

        registrationRequest.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        userRepository.save(userMapper.toEntity(registrationRequest));
    }
}
