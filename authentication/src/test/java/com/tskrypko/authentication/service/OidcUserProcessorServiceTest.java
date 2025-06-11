package com.tskrypko.authentication.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tskrypko.authentication.mapper.UserMapper;
import com.tskrypko.authentication.model.OidcUserAttributes;
import com.tskrypko.authentication.model.User;
import com.tskrypko.authentication.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OidcUserProcessorServiceTest {

    @InjectMocks
    private OidcUserProcessorService processorService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private User existingUser;

    @Mock
    private User newUser;

    @Test
    public void processUser_shouldReturnNull_whenUserExists() {
        // Arrange
        OidcUserAttributes attributes = new OidcUserAttributes(
                "existing@example.com", "Existing", "google", "user123");

        when(userRepository.findByEmail(attributes.email())).thenReturn(Optional.of(existingUser));
        when(existingUser.getProvider()).thenReturn("google"); // Already set

        // Act
        User result = processorService.processUser(attributes);

        // Assert
        assertNull(result);
        verify(userRepository, never()).save(any(User.class)); // No save should happen
    }

    @Test
    public void processUser_shouldUpdateProvider_whenUserExistsWithoutProvider() {
        // Arrange
        OidcUserAttributes attributes = new OidcUserAttributes(
                "existing@example.com", "Existing", "google", "user123");

        when(userRepository.findByEmail(attributes.email())).thenReturn(Optional.of(existingUser));
        when(existingUser.getProvider()).thenReturn(null); // Provider not set

        // Act
        User result = processorService.processUser(attributes);

        // Assert
        assertNull(result);
        verify(existingUser).setProvider("google");
        verify(existingUser).setProviderId("user123");
        verify(userRepository).save(existingUser);
    }

    @Test
    public void processUser_shouldCreateNewUser_whenUserDoesNotExist() {
        // Arrange
        OidcUserAttributes attributes = new OidcUserAttributes(
                "new@example.com", "New", "google", "user456");

        when(userRepository.findByEmail(attributes.email())).thenReturn(Optional.empty());
        when(userMapper.createOAuth2User(
                eq(attributes.email()),
                eq(attributes.name()),
                eq(attributes.provider()),
                eq(attributes.providerId())
        )).thenReturn(newUser);
        when(userRepository.save(newUser)).thenReturn(newUser);

        // Act
        User result = processorService.processUser(attributes);

        // Assert
        assertSame(newUser, result);
        verify(newUser).setPassword("OAUTH2_ONLY");
        verify(userRepository).save(newUser);
    }
}
