package com.tskrypko.authentication.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationFlowFeaturesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void loginPageWithoutPKCE_shouldRedirectToFrontend() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/login"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("http*://**"));
    }

    @Test
    public void loginPageWithError_shouldDisplayLoginPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/login")
                .param("error", "invalid"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/login"))
                .andExpect(model().attribute("hasError", true))
                .andExpect(model().attribute("errorType", "invalid"));
    }

    @Test
    public void loginPageWithNotVerifiedError_shouldDisplayAppropriateError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/login")
                .param("error", "not_verified"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/login"))
                .andExpect(model().attribute("errorType", "not_verified"));
    }

    @Test
    public void logout_shouldClearCookies() throws Exception {
        Cookie refreshTokenCookie = new Cookie("refresh_token", "mock-refresh-token");

        MvcResult logoutResult = mockMvc.perform(post("/oauth2/logout")
                .cookie(refreshTokenCookie)
                .with(csrf()))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpServletResponse response = logoutResult.getResponse();
        Cookie clearedCookie = response.getCookie("refresh_token");
        assertNotNull(clearedCookie);
        assertEquals(0, clearedCookie.getMaxAge());
    }
}
