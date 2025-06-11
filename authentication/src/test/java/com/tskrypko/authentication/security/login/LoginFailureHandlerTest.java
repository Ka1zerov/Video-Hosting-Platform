package com.tskrypko.authentication.security.login;

import com.tskrypko.authentication.config.security.login.LoginFailureHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;

import static org.mockito.Mockito.*;

class LoginFailureHandlerTest {

    private LoginFailureHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = spy(new LoginFailureHandler());
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
    }

    @Test
    void shouldUseInvalidErrorForExceptions() throws IOException, ServletException {
        AuthenticationException exception = new BadCredentialsException("Bad credentials");

        doCallRealMethod().when(handler).onAuthenticationFailure(any(), any(), any());

        handler.onAuthenticationFailure(request, response, exception);

        verify(handler).setDefaultFailureUrl("/login?error=invalid");
    }
}
