package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.auth.login.AtrilhaOAuth2User;
import dev.zayt.atrilha.auth.login.LoginAccountQuery;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Stubs/beans de teste para {@code OAuth2GoogleSignupChainIT}.
 *
 * <p>Vive no pacote {@code dev.zayt.atrilha.auth} para poder estender
 * {@link GoogleOAuth2UserService}, que e package-private. O IT em si vive em
 * {@code dev.zayt.atrilha.auth.web} (conforme determinado pela issue #78).</p>
 *
 * <p>Provê:
 * <ul>
 *   <li>{@link StubGoogleOAuth2UserService} marcado {@link Primary} para
 *       substituir o servico real no contexto Spring (sem chamar o Google).</li>
 *   <li>{@link OAuth2AccessTokenResponseClient} stubado para retornar um token
 *       falso, evitando HTTP real ao endpoint de token do Google. O bean e
 *       auto-discovered pelo {@code OAuth2LoginConfigurer} via ResolvableType.</li>
 * </ul></p>
 */
@TestConfiguration
public class OAuth2GoogleSignupChainITStubs {

    @Bean
    @Primary
    public StubGoogleOAuth2UserService stubGoogleOAuth2UserService(LoginAccountQuery loginAccountQuery) {
        return new StubGoogleOAuth2UserService(loginAccountQuery);
    }

    /**
     * Stub do client de troca de codigo por token. Devolve uma access token
     * falsa para que a chain do Spring Security progrida ate o
     * {@code userInfoEndpoint} sem precisar de HTTP real ao Google.
     */
    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> stubAccessTokenResponseClient() {
        return request -> OAuth2AccessTokenResponse.withToken("fake-token")
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .expiresIn(3600)
                .build();
    }

    /**
     * Substitui o {@link GoogleOAuth2UserService} real por um stub que devolve
     * atributos OAuth controlados via {@link ThreadLocal}, sem chamar o Google.
     *
     * <p>Como o servico real e package-private, esta classe vive no mesmo
     * pacote {@code dev.zayt.atrilha.auth}.</p>
     *
     * <p>A logica de negocio (validar email_verified, normalizar email,
     * consultar LoginAccountQuery) e reproduzida aqui propositalmente — o
     * objetivo do IT e exercitar a chain do Spring Security, nao re-testar
     * o {@code GoogleOAuth2UserService} (isso ja e coberto pelo
     * {@code GoogleOAuth2UserServiceTest}).</p>
     */
    public static class StubGoogleOAuth2UserService extends GoogleOAuth2UserService {

        private final LoginAccountQuery loginAccountQuery;
        private final ThreadLocal<Map<String, Object>> attributesHolder = new ThreadLocal<>();
        private final ThreadLocal<Boolean> cancelException = new ThreadLocal<>();
        private final ThreadLocal<String> lastPendingEmail = new ThreadLocal<>();
        private final ThreadLocal<Map<String, Boolean>> hasGuardianLinkOverride = new ThreadLocal<>();

        StubGoogleOAuth2UserService(LoginAccountQuery loginAccountQuery) {
            super(loginAccountQuery);
            this.loginAccountQuery = loginAccountQuery;
        }

        public void setAttributes(Map<String, Object> attrs) {
            attributesHolder.set(attrs);
            cancelException.remove();
        }

        public void setCancelException(boolean cancel) {
            cancelException.set(cancel);
            attributesHolder.remove();
        }

        public String getLastPendingEmail() {
            return lastPendingEmail.get();
        }

        public void setHasGuardianLinkOverride(String email, boolean hasLink) {
            Map<String, Boolean> overrides = hasGuardianLinkOverride.get();
            if (overrides == null) {
                overrides = new HashMap<>();
                hasGuardianLinkOverride.set(overrides);
            }
            overrides.put(email.toLowerCase(Locale.ROOT), hasLink);
        }

        public void reset() {
            attributesHolder.remove();
            cancelException.remove();
            lastPendingEmail.remove();
            hasGuardianLinkOverride.remove();
        }

        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) {
            if (Boolean.TRUE.equals(cancelException.get())) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("access_denied", "user cancelled", null));
            }

            Map<String, Object> attrs = attributesHolder.get();
            if (attrs == null) {
                throw new IllegalStateException(
                        "StubGoogleOAuth2UserService: nenhum atributo configurado. "
                                + "Chame setAttributes() antes da request OAuth.");
            }

            // 1. Validar email_verified (mesma logica do servico real)
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

            // 3. Consultar conta — aplica override de hasGuardianLink se configurado
            Optional<LoginAccountQuery.LoginAccount> accountOpt = loginAccountQuery.findForLogin(email);

            if (accountOpt.isPresent()) {
                LoginAccountQuery.LoginAccount account = accountOpt.get();
                Map<String, Boolean> overrides = hasGuardianLinkOverride.get();
                if (overrides != null && overrides.containsKey(email)) {
                    boolean hasLink = overrides.get(email);
                    LoginAccountQuery.LoginAccount modified = new LoginAccountQuery.LoginAccount(
                            account.email(),
                            account.passwordHashBcrypt(),
                            account.role(),
                            hasLink,
                            account.displayName());
                    return new AtrilhaOAuth2User(modified, attrs);
                }
                return new AtrilhaOAuth2User(account, attrs);
            }

            // Conta nao existe -> pending signup (novo contrato do FIX-015)
            lastPendingEmail.set(email);
            return AtrilhaOAuth2User.pendingSignup(email, attrs);
        }
    }
}
