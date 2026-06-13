package com.relayflow.api.authentication;

import com.relayflow.api.authentication.dto.AuthenticatedUserResponse;
import com.relayflow.api.authentication.dto.GuestRecoveryRequest;
import com.relayflow.api.authentication.dto.GuestSessionResponse;
import com.relayflow.api.authentication.dto.Login2FARequest;
import com.relayflow.api.authentication.dto.LoginRequest;
import com.relayflow.api.authentication.dto.SignupRequest;
import com.relayflow.api.authentication.dto.SignupResponse;
import com.relayflow.api.authentication.dto.VerifyEmailRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    private final String sessionCookieName;

    private final boolean sessionCookieSecure;

    public AuthenticationController(
            AuthenticationService authenticationService,
            @Value("${server.servlet.session.cookie.name:JSESSIONID}") String sessionCookieName,
            @Value("${server.servlet.session.cookie.secure:false}") boolean sessionCookieSecure) {
        this.authenticationService = authenticationService;
        this.sessionCookieName = sessionCookieName;
        this.sessionCookieSecure = sessionCookieSecure;
    }

    @GetMapping("/me")
    AuthenticatedUserResponse me(Authentication authentication) {
        return authenticationService.getCurrentUser(authentication);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.ACCEPTED)
    SignupResponse signup(
            @Valid @RequestBody SignupRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return authenticationService.signup(request, authentication, httpRequest, httpResponse);
    }

    @PostMapping("/verify-email")
    AuthenticatedUserResponse verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return authenticationService.verifyEmail(request.token(), httpRequest, httpResponse);
    }

    @PostMapping("/login")
    AuthenticatedUserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return authenticationService.login(request, httpRequest, httpResponse);
    }

    @PostMapping("/login/2fa")
    AuthenticatedUserResponse login2FA(
            @Valid @RequestBody Login2FARequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return authenticationService.login2FA(request, httpRequest, httpResponse);
    }

    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    GuestSessionResponse guest(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return authenticationService.createGuestSession(httpRequest, httpResponse);
    }

    @PostMapping("/guest/recover")
    GuestSessionResponse recoverGuest(
            @Valid @RequestBody GuestRecoveryRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return authenticationService.recoverGuestSession(
                request.recoveryToken(), httpRequest, httpResponse);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        // Expire the session cookie so the browser removes it immediately.
        // Without this the cookie survives in the browser and the Next.js
        // middleware — which checks cookie presence — keeps redirecting the
        // user back to /inbox after logout.
        Cookie cookie = new Cookie(sessionCookieName, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(sessionCookieSecure);
        response.addCookie(cookie);
    }
}
