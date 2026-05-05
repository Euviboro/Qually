package com.qually.qually.security;

import com.qually.qually.models.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Called by Spring Security after a successful OAuth2 login.
 *
 * <p>Responsibilities:</p>
 * <ol>
 *   <li>Extracts the Qually {@link User} from the {@link QuallyOAuth2User} principal.</li>
 *   <li>Mints an access token + refresh token using {@link JwtService} —
 *       the same cookie format the rest of the app already expects.</li>
 *   <li>Sets both tokens as {@code httpOnly; SameSite=Strict} cookies.</li>
 *   <li>Redirects the browser to the frontend root so React can boot and call
 *       {@code GET /api/auth/me} to hydrate the auth context.</li>
 * </ol>
 *
 * <p>On failure (user not in DB, inactive) the {@link CustomOAuth2UserService}
 * throws an {@link org.springframework.security.oauth2.core.OAuth2AuthenticationException}
 * which Spring redirects to {@code /login?error}. The frontend reads the
 * {@code error} query param and shows a friendly message.</p>
 */
@Component
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuthSuccessHandler.class);

    private static final String ACCESS_TOKEN_COOKIE  = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final JwtService jwtService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public OAuthSuccessHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        QuallyOAuth2User principal = (QuallyOAuth2User) authentication.getPrincipal();
        User user = principal.getQuallyUser();

        // Mint JWT cookies — same shape the JwtFilter already validates
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        response.addHeader("Set-Cookie", buildCookie(
                ACCESS_TOKEN_COOKIE,
                accessToken,
                jwtService.getAccessTokenExpiryMs() / 1000));

        response.addHeader("Set-Cookie", buildCookie(
                REFRESH_TOKEN_COOKIE,
                refreshToken,
                jwtService.getRefreshTokenExpiryMs() / 1000));

        log.info("OAuth2 login complete for user {} — redirecting to frontend",
                user.getUserId());

        // Redirect browser to the React app; it will call /api/auth/me to hydrate
        response.sendRedirect(frontendUrl);
    }

    private String buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Strict")
                .secure(cookieSecure)
                .build()
                .toString();
    }

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;
}