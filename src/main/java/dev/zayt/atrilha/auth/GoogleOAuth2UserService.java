package dev.zayt.atrilha.auth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Envelopa {@link DefaultOAuth2UserService} para aplicar a regra "o Google
 * deve nos dizer que o e-mail foi verificado" (RF-E1-07, US-002).
 *
 * <p>Se {@code email_verified} nao for verdadeiro, lanca
 * {@link OAuth2AuthenticationException} com codigo {@code email_unverified} —
 * o {@code OAuthFailureHandler} traduz para o redirect apropriado com
 * mensagem amigavel para Julia.</p>
 *
 * <p>Visibilidade package-private; o {@code SecurityConfig} resolve via DI.</p>
 */
@Component
class GoogleOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User raw = super.loadUser(userRequest);
        Map<String, Object> attrs = raw.getAttributes();
        if (!Boolean.TRUE.equals(attrs.get("email_verified"))) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(
                            "email_unverified",
                            "E-mail Google nao verificado",
                            null));
        }
        return raw;
    }
}
