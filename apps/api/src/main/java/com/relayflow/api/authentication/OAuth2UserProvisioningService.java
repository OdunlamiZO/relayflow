package com.relayflow.api.authentication;

import com.relayflow.api.authentication.domain.AuthenticationProvider;
import com.relayflow.api.authentication.domain.User;
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

    public OAuth2UserProvisioningService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

        User user =
                userRepository
                        .findByProviderIdentity(AuthenticationProvider.GOOGLE, subject)
                        .orElseGet(User::new);

        boolean isNew = user.getId() == null;

        user.setProvider(AuthenticationProvider.GOOGLE);
        user.setProviderSubject(subject);
        user.setEmail(email);
        user.setDisplayName(stringAttribute(attributes, "name"));
        user.setAvatarUrl(stringAttribute(attributes, "picture"));

        userRepository.save(user);

        if (isNew) {
            log.info("Provisioned new Google user: email={}", email);
        } else {
            log.debug("Updated existing Google user: email={}", email);
        }
    }

    private String stringAttribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);

        return value == null ? null : value.toString();
    }
}
