package com.relayflow.api.authentication;

import com.relayflow.api.authentication.domain.AuthenticationProvider;
import com.relayflow.api.authentication.domain.User;
import com.relayflow.api.authentication.domain.UserIdentity;
import com.relayflow.api.authentication.domain.UserPreferences;
import com.relayflow.api.authentication.repository.UserIdentityRepository;
import com.relayflow.api.authentication.repository.UserPreferencesRepository;
import com.relayflow.api.authentication.repository.UserRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuth2UserProvisioningService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(OAuth2UserProvisioningService.class);

    private final UserRepository userRepository;

    private final UserIdentityRepository identityRepository;

    private final UserPreferencesRepository prefsRepository;

    public OAuth2UserProvisioningService(
            UserRepository userRepository,
            UserIdentityRepository identityRepository,
            UserPreferencesRepository prefsRepository) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.prefsRepository = prefsRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);

        if ("google".equals(userRequest.getClientRegistration().getRegistrationId())) {
            provisionGoogleUser(oauthUser.getAttributes());
        }

        return oauthUser;
    }

    private void provisionGoogleUser(Map<String, Object> attributes) {
        String subject = stringAttribute(attributes, "sub");
        String email = stringAttribute(attributes, "email");

        UserIdentity identity =
                identityRepository
                        .findByProviderAndProviderSubject(AuthenticationProvider.GOOGLE, subject)
                        .orElseGet(
                                () -> {
                                    User newUser = new User();
                                    newUser.setEmail(email);
                                    userRepository.save(newUser);

                                    UserPreferences prefs = new UserPreferences();
                                    prefs.setUser(newUser);
                                    prefsRepository.save(prefs);

                                    UserIdentity newIdentity = new UserIdentity();
                                    newIdentity.setUser(newUser);
                                    newIdentity.setProvider(AuthenticationProvider.GOOGLE);
                                    newIdentity.setProviderSubject(subject);
                                    newIdentity.setVerified(true);

                                    log.info("Provisioned new Google user: email={}", email);

                                    return identityRepository.save(newIdentity);
                                });

        // Update profile fields that the provider may have changed since last login.
        User user = identity.getUser();
        user.setEmail(email);
        user.setDisplayName(stringAttribute(attributes, "name"));
        user.setAvatarUrl(stringAttribute(attributes, "picture"));
        userRepository.save(user);
    }

    private String stringAttribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);

        return value == null ? null : value.toString();
    }
}
