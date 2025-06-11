package com.tskrypko.authentication.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.ui.Model;
import org.springframework.web.servlet.view.RedirectView;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginControllerTest {

    private LoginController controller;
    private Model model;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSessionRequestCache requestCache;
    private SavedRequest savedRequest;

    @BeforeEach
    void setUp() {
        controller = new LoginController();

        // Using reflection to set the frontend URL
        try {
            java.lang.reflect.Field field = LoginController.class.getDeclaredField("frontendUrl");
            field.setAccessible(true);
            field.set(controller, "http://localhost:3000");

            // Replace the RequestCache with a mock
            field = LoginController.class.getDeclaredField("requestCache");
            field.setAccessible(true);
            requestCache = mock(HttpSessionRequestCache.class);
            field.set(controller, requestCache);
        } catch (Exception e) {
            e.printStackTrace();
        }

        model = mock(Model.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        savedRequest = mock(SavedRequest.class);
    }

    @Test
    void shouldShowLoginPageWhenErrorParameterIsPresent() {
        when(request.getParameter("error")).thenReturn("invalid");

        Object result = controller.loginPage(model, request, response);

        assertEquals("authentication/login", result);
        verify(model).addAttribute("frontendUrl", "http://localhost:3000");
        verify(model).addAttribute("errorType", "invalid");
        verify(model).addAttribute("hasError", true);
    }

    @Test
    void shouldShowLoginPageWhenPkceParametersArePresent() {
        when(request.getParameter("error")).thenReturn(null);
        when(requestCache.getRequest(request, response)).thenReturn(savedRequest);
        when(savedRequest.getRedirectUrl()).thenReturn("https://example.com/callback?code_challenge=abc123");

        Object result = controller.loginPage(model, request, response);

        assertEquals("authentication/login", result);
        verify(model).addAttribute("frontendUrl", "http://localhost:3000");
    }

    @Test
    void shouldRedirectToFrontendWhenNoPkceParameters() {
        when(request.getParameter("error")).thenReturn(null);
        when(requestCache.getRequest(request, response)).thenReturn(savedRequest);
        when(savedRequest.getRedirectUrl()).thenReturn("https://example.com/callback");

        Object result = controller.loginPage(model, request, response);

        assertInstanceOf(RedirectView.class, result);
        RedirectView redirectView = (RedirectView) result;
        assertEquals("http://localhost:3000", redirectView.getUrl());
    }

    @Test
    void shouldRedirectToFrontendWhenNoSavedRequest() {
        when(request.getParameter("error")).thenReturn(null);
        when(requestCache.getRequest(request, response)).thenReturn(null);

        Object result = controller.loginPage(model, request, response);

        assertInstanceOf(RedirectView.class, result);
        RedirectView redirectView = (RedirectView) result;
        assertEquals("http://localhost:3000", redirectView.getUrl());
    }
}
