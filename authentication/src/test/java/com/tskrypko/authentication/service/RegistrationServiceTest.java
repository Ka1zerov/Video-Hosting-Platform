package com.tskrypko.authentication.service;

import com.tskrypko.authentication.dto.RegistrationRequest;
import com.tskrypko.authentication.exception.UserAlreadyExistsException;
import com.tskrypko.authentication.mapper.UserMapper;
import com.tskrypko.authentication.model.User;
import com.tskrypko.authentication.repository.UserRepository;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private AuthService authService;

    private final String userName = "testUser";

    private final String userEmail = "test@example.com";

    private final String userPassword = "password123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Nested
    @DisplayName("Registration tests")
    public class RegistrationTests{
        @Test
        void testRegister_withNewUser() {
            RegistrationRequest registrationRequest = new RegistrationRequest();
            registrationRequest.setUsername(userName);
            registrationRequest.setPassword(userPassword);
            registrationRequest.setEmail(userEmail);
            registrationRequest.setFirstName("test");
            registrationRequest.setLastName("user");

            User user = new User();
            user.setId(UUID.randomUUID());
            user.setUsername(userName);
            user.setEmail(userEmail);
            user.setPassword(userPassword);

            when(userRepository.existsByEmail(userEmail)).thenReturn(false);
            when(passwordEncoder.encode(userPassword)).thenReturn("encodedPassword");
            when(userMapper.toEntity(registrationRequest)).thenReturn(user);
            when(userRepository.save(any())).thenReturn(user);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> userRepository.save(user));

            authService.register(registrationRequest);

            verify(userRepository, times(1)).save(any());
    }

        @Test
        void testRegister_withExistingUser() {
            RegistrationRequest registrationRequest = new RegistrationRequest();
            registrationRequest.setEmail(userEmail);

            when(userRepository.existsByEmail(userEmail)).thenReturn(true);

            assertThrows(UserAlreadyExistsException.class, () -> authService.register(registrationRequest));
        }
    }
}
