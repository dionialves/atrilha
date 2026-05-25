package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.auth.login.AtrilhaOAuth2User;
import dev.zayt.atrilha.auth.login.LoginAccountQuery;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Envelopa {@link DefaultOAuth2UserService} para aplicar a regra "o Google
 * deve nos dizer que o e-mail foi verificado" (RF-E1-07, US-002) e promover
 * o {@code OAuth2User} cru para um {@link AtrilhaOAuth2User} com authorities
 * derivadas da conta no banco.
 *
 * <p>Se {@code email_verified} nao for verdadeiro, lanca
 * {@link OAuth2AuthenticationException} com codigo {@code email_unverified}.
 * Se o e-mail nao corresponde a nenhuma conta cadastrada, devolve um
 * {@link AtrilhaOAuth2User} em estado {@code PENDING_SIGNUP} (sem role efetiva,
 * {@code account == null}). O dispatcher no success handler identifica esse
 * estado e encaminha para o fluxo de cadastro novo ({@code /cadastro/adolescente/complementar}).</p>
 *
 * <p>O {@link AtrilhaOAuth2User} retornado implementa {@link AuthenticatedPrincipal},
 * garantindo que o principal no SecurityContext tenha a authority correta
 * ({@code ROLE_TEEN} ou {@code ROLE_GUARDIAN}) e nao so {@code OAUTH2_USER}.
 * Isso evita o bug de 403 em rotas protegidas por papel.</p>
 *
 * <p>Visibilidade package-private; o {@code SecurityConfig} resolve via DI.</p>
 */
@Component
class GoogleOAuth2UserService extends DefaultOAuth2UserService {

    private final LoginAccountQuery loginAccountQuery;

    GoogleOAuth2UserService(LoginAccountQuery loginAccountQuery) {
        this.loginAccountQuery = loginAccountQuery;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User raw = super.loadUser(userRequest);
        Map<String, Object> attrs = raw.getAttributes();

        // 1. Validar email_verified (preserva logica atual — erro precede consulta)
        if (!Boolean.TRUE.equals(attrs.get("email_verified"))) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(
                            "email_unverified",
                            "E-mail Google nao verificado",
                            null));
        }

        // 2. Extrair e normalizar email
        String rawEmail = (String) attrs.get("email");
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(
                            "missing_email",
                            "E-mail Google nao fornecido",
                            null));
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);

        // 3. Consultar conta no banco — se nao existe, devolve principal PENDING_SIGNUP
        return loginAccountQuery.findForLogin(email)
                .map(account -> (OAuth2User) new AtrilhaOAuth2User(account, attrs))
                .orElseGet(() -> (OAuth2User) AtrilhaOAuth2User.pendingSignup(email, attrs));
    }
}
