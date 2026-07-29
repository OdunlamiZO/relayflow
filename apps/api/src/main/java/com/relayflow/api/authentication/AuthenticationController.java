package com.relayflow.api.authentication;

import com.relayflow.api.authentication.dto.AuthenticatedUserResponse;
import com.relayflow.api.authentication.dto.BootstrapRequest;
import com.relayflow.api.authentication.dto.BootstrapResponse;
import com.relayflow.api.authentication.dto.InstanceStatusResponse;
import com.relayflow.api.authentication.dto.Login2FARequest;
import com.relayflow.api.authentication.dto.LoginRequest;
import com.relayflow.api.authentication.dto.ResetPasswordRequest;
import com.relayflow.api.authentication.dto.SignupRequest;
import com.relayflow.api.authentication.dto.SignupResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    private final PasswordResetService passwordResetService;

    private final String sessionCookieName;

    private final boolean sessionCookieSecure;

    public AuthenticationController(
            AuthenticationService authenticationService,
            PasswordResetService passwordResetService,
            @Value("${server.servlet.session.cookie.name:JSESSIONID}") String sessionCookieName,
            @Value("${server.servlet.session.cookie.secure:false}") boolean sessionCookieSecure) {
        this.authenticationService = authenticationService;
        this.passwordResetService = passwordResetService;
        this.sessionCookieName = sessionCookieName;
        this.sessionCookieSecure = sessionCookieSecure;
    }

    @GetMapping("/me")
    AuthenticatedUserResponse me(Authentication authentication) {
        return authenticationService.getCurrentUser(authentication);
    }

    @GetMapping("/bootstrap-status")
    InstanceStatusResponse bootstrapStatus() {
        return authenticationService.getInstanceStatus();
    }

    @PostMapping("/bootstrap")
    BootstrapResponse bootstrap(
            @Valid @RequestBody BootstrapRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return authenticationService.bootstrap(request, httpRequest, httpResponse);
    }

    @PostMapping("/signup")
    SignupResponse signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return authenticationService.signup(request, httpRequest, httpResponse);
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

    @PostMapping("/reset-password/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@PathVariable UUID token, @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(token, request.newPassword());
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
