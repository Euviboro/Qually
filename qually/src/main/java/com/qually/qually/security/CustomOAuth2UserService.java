package com.qually.qually.security;

import com.qually.qually.models.User;
import com.qually.qually.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Custom OIDC user service for Azure AD.
 *
 * <p>Azure AD uses OIDC (openid scope), so Spring calls this service —
 * NOT DefaultOAuth2UserService. We delegate to Spring's built-in
 * {@link OidcUserService} to handle the token exchange and claim parsing,
 * then wrap the result in {@link QuallyOAuth2User} which carries the matched
 * DB user so {@link OAuthSuccessHandler} can mint the JWT without a second
 * DB call.</p>
 */
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final OidcUserService delegate = new OidcUserService();
    private final UserRepository  userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);

        String email = extractEmail(oidcUser);

        if (email == null || email.isBlank()) {
            log.warn("OIDC login attempt with no email claim");
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "Microsoft account did not return an email address.");
        }

        User quallyUser = userRepository
                .findByUserEmailWithRole(email.trim().toLowerCase())
                .orElse(null);

        if (quallyUser == null) {
            log.warn("OIDC login rejected — email '{}' not in Qually", email);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_not_registered"),
                    "Your account (%s) is not registered in Qually.".formatted(email));
        }

        if (!Boolean.TRUE.equals(quallyUser.getIsActive())) {
            log.warn("OIDC login rejected — user {} is inactive", quallyUser.getUserId());
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_inactive"),
                    "Your Qually account is inactive.");
        }

        log.info("OIDC login accepted for user {} ({})", quallyUser.getUserId(), email);
        return new QuallyOAuth2User(oidcUser, quallyUser);
    }

    private String extractEmail(OidcUser user) {
        String email = user.getEmail();
        if (email != null && !email.isBlank()) return email;

        Object upn = user.getAttribute("preferred_username");
        if (upn instanceof String s && !s.isBlank()) return s;

        return null;
    }
}