package com.relayflow.api.profile;

import com.relayflow.api.authentication.SecurityUtils;
import com.relayflow.api.profile.dto.ChangePasswordRequest;
import com.relayflow.api.profile.dto.DeleteAccountRequest;
import com.relayflow.api.profile.dto.OtpRequest;
import com.relayflow.api.profile.dto.ProfileResponse;
import com.relayflow.api.profile.dto.Setup2FAResponse;
import com.relayflow.api.profile.dto.UpdateProfileRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    private final SecurityUtils securityUtils;

    public ProfileController(ProfileService profileService, SecurityUtils securityUtils) {
        this.profileService = profileService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    ProfileResponse getProfile(Authentication authentication) {
        return profileService.getProfile(currentUserId(authentication));
    }

    @PatchMapping
    ProfileResponse updateProfile(
            @Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
        return profileService.updateProfile(currentUserId(authentication), request);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(
            @Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        profileService.changePassword(currentUserId(authentication), request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAccount(
            @RequestBody(required = false) DeleteAccountRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        profileService.deleteAccount(
                currentUserId(authentication),
                request != null ? request : new DeleteAccountRequest(null));

        // Invalidate session after account deletion.
        HttpSession session = httpRequest.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        httpResponse.addCookie(cookie);
    }

    // ── 2FA ──────────────────────────────────────────────────────────────────

    @PostMapping("/2fa/setup")
    Setup2FAResponse setup2FA(Authentication authentication) {
        return profileService.setup2FA(currentUserId(authentication));
    }

    @PostMapping("/2fa/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void enable2FA(@Valid @RequestBody OtpRequest request, Authentication authentication) {
        profileService.enable2FA(currentUserId(authentication), request);
    }

    @PostMapping("/2fa/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void disable2FA(@Valid @RequestBody OtpRequest request, Authentication authentication) {
        profileService.disable2FA(currentUserId(authentication), request);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UUID currentUserId(Authentication authentication) {
        return securityUtils.resolveUserId(authentication);
    }
}
