package com.qually.qually.security;

import com.qually.qually.models.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Wraps a Spring {@link OidcUser} (from Azure AD) with the matched Qually
 * {@link User} entity.
 *
 * <p>Must implement {@link OidcUser} — not just {@link org.springframework.security.oauth2.core.user.OAuth2User} —
 * because Azure AD triggers the OIDC flow (openid scope), so Spring's
 * principal is always an {@link OidcUser}. {@link OAuthSuccessHandler}
 * casts the principal to this type to retrieve the DB user.</p>
 */
public class QuallyOAuth2User implements OidcUser {

    private final OidcUser delegate;
    private final User     quallyUser;

    public QuallyOAuth2User(OidcUser delegate, User quallyUser) {
        this.delegate   = delegate;
        this.quallyUser = quallyUser;
    }

    /** The matched Qually DB user — used by {@link OAuthSuccessHandler} to issue the JWT. */
    public User getQuallyUser() {
        return quallyUser;
    }

    // ── OidcUser ─────────────────────────────────────────────

    @Override public Map<String, Object> getClaims()   { return delegate.getClaims();   }
    @Override public OidcUserInfo getUserInfo()         { return delegate.getUserInfo(); }
    @Override public OidcIdToken  getIdToken()          { return delegate.getIdToken();  }

    // ── OAuth2User ────────────────────────────────────────────

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return quallyUser.getUserEmail();
    }
}