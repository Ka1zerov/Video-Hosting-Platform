package com.tskrypko.authentication.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Controller responsible for handling login requests.
 * It determines whether to display the login page or redirect the user to the frontend.
 *
 * The login flow works as follows:
 * 1. When a user attempts to access a protected resource, they may be redirected to the login endpoint.
 * 2. If a previously saved request exists (indicating an OAuth2 authorization attempt), the controller checks for PKCE parameters.
 * 3. If PKCE parameters are detected, the login page is displayed to allow secure authentication.
 * 4. If no saved request or PKCE parameters exist, the user is redirected to the frontend to initiate a proper authentication flow.
 */
@Controller
public class LoginController {

    @Value("${frontend.base_url}")
    private String frontendUrl;

    private final RequestCache requestCache = new HttpSessionRequestCache();

    /**
     * Handles the login request.
     *
     * @param model the model to store attributes
     * @param request the HTTP request
     * @param response the HTTP response
     * @return the login page if the request follows the PKCE flow, otherwise a redirect to the frontend
     *
     * Logic:
     * - If there is a saved request (indicating an OAuth2 flow), check if it contains PKCE parameters.
     * - If PKCE parameters exist, return the login page to allow secure authentication.
     * - If no PKCE parameters exist, or if the request was made directly, redirect to the frontend.
     */
    @GetMapping("/login")
    public Object loginPage(Model model, HttpServletRequest request, HttpServletResponse response) {
        SavedRequest savedRequest = requestCache.getRequest(request, response);

        model.addAttribute("frontendUrl", frontendUrl);

        String errorParam = request.getParameter("error");
        if (errorParam != null) {
            model.addAttribute("errorType", errorParam);
            model.addAttribute("hasError", true);

            System.out.println("Error parameter detected: " + errorParam);

            return "authentication/login";
        }

        if (savedRequest != null) {
            String redirectUrl = savedRequest.getRedirectUrl();
            boolean hasPkceParameters = redirectUrl.contains("code_challenge=");

            if (hasPkceParameters) {
                return "authentication/login";
            }
        }

        return new RedirectView(frontendUrl);
    }
}
