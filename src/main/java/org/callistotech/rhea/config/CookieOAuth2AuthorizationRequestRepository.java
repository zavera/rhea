package org.callistotech.rhea.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

/**
 * Stores the OAuth2 authorization request (including state) in a short-lived cookie instead of
 * the HTTP session. This survives the cross-site redirect from Google back to the app even when
 * a proxy in front of the app (e.g. Railway's) interferes with session cookies.
 */
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "oauth2_auth_req";
    private static final int MAX_AGE_SECONDS = 180;

    private final ThreadLocal<Boolean> currentRequestIsHttps = ThreadLocal.withInitial(() -> false);

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return readCookie(request);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request, HttpServletResponse response) {
        currentRequestIsHttps.set(isHttps(request));
        if (authorizationRequest == null) {
            clearCookie(response);
            return;
        }
        response.addHeader("Set-Cookie",
                buildCookieHeader(serialize(authorizationRequest), MAX_AGE_SECONDS));
        currentRequestIsHttps.remove();
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
            HttpServletResponse response) {
        OAuth2AuthorizationRequest req = loadAuthorizationRequest(request);
        currentRequestIsHttps.set(isHttps(request));
        clearCookie(response);
        currentRequestIsHttps.remove();
        return req;
    }

    private boolean isHttps(HttpServletRequest request) {
        return "https".equalsIgnoreCase(request.getScheme())
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    private OAuth2AuthorizationRequest readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName())) {
                return deserialize(c.getValue());
            }
        }
        return null;
    }

    private void clearCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookieHeader("", 0));
    }

    private String buildCookieHeader(String value, int maxAge) {
        // Secure + SameSite=None is required for the cross-site redirect over HTTPS (prod).
        // Over plain HTTP (local dev) the Secure flag causes the browser to drop the cookie.
        boolean secure = currentRequestIsHttps.get();
        String header = COOKIE_NAME + "=" + value
                + "; Path=/"
                + "; HttpOnly"
                + "; Max-Age=" + maxAge;
        if (secure) header += "; Secure; SameSite=None";
        return header;
    }

    private String serialize(OAuth2AuthorizationRequest req) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(req);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize OAuth2AuthorizationRequest", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (OAuth2AuthorizationRequest) ois.readObject();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
