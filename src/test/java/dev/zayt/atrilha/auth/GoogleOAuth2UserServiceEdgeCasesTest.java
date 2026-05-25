package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.auth.login.LoginAccountQuery;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

/**
 * Bordas e claims malformadas do {@link GoogleOAuth2UserService} (US-002).
 *
 * <p>Complementa o {@code GoogleOAuth2UserServiceTest} do plano. Foco:
 * defesa em profundidade contra claims que o Google "nunca" envia, mas
 * que se aparecerem (provider de teste, IdP federado, bug do Google)
 * NAO devem permitir a entrada no fluxo de cadastro com e-mail nao
 * comprovadamente verificado.</p>
 */
class GoogleOAuth2UserServiceEdgeCasesTest {

    private GoogleOAuth2UserService stubbedWith(OAuth2User upstream) {
        LoginAccountQuery loginAccountQuery = mock(LoginAccountQuery.class);
        return new GoogleOAuth2UserService(loginAccountQuery) {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) {
                Map<String, Object> a = upstream.getAttributes();
                if (!Boolean.TRUE.equals(a.get("email_verified"))) {
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error("email_unverified",
                                    "E-mail Google nao verificado", null));
                }
                return upstream;
            }
        };
    }

    @Test
    void rejeitaEmailVerifiedComStringTrueEmVezDeBooleano() {
        // Google sempre manda Boolean, mas defesa: se vier "true" como
        // String (por bug ou IdP federado mal configurado), NAO aceitar.
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "str-true");
        attrs.put("email", "str@gmail.com");
        attrs.put("email_verified", "true"); // String, nao Boolean
        OAuth2User stubbed = new DefaultOAuth2User(
                createAuthorityList("OAUTH2_USER"), attrs, "sub");

        GoogleOAuth2UserService service = stubbedWith(stubbed);

        assertThatThrownBy(() -> service.loadUser(null))
                .as("comparacao deve ser estrita Boolean.TRUE.equals — String 'true' NAO eh true")
                .isInstanceOf(OAuth2AuthenticationException.class)
                .matches(ex -> ((OAuth2AuthenticationException) ex)
                        .getError().getErrorCode().equals("email_unverified"));
    }

    @Test
    void rejeitaEmailVerifiedComBooleanoNulo() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "null-bool");
        attrs.put("email", "null@gmail.com");
        attrs.put("email_verified", null);
        OAuth2User stubbed = new DefaultOAuth2User(
                createAuthorityList("OAUTH2_USER"), attrs, "sub");

        GoogleOAuth2UserService service = stubbedWith(stubbed);

        assertThatThrownBy(() -> service.loadUser(null))
                .as("email_verified null deve ser tratado como nao-verificado")
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void contractDoEmailUnverifiedErrorCodeEhEstavel() {
        // O OAuthFailureHandler depende deste error code para classificar
        // a falha como toast "email_unverified". Se alguem trocar o
        // string aqui, o handler quebra silenciosamente.
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "ctr");
        attrs.put("email", "x@y.com");
        attrs.put("email_verified", false);
        OAuth2User stubbed = new DefaultOAuth2User(
                createAuthorityList("OAUTH2_USER"), attrs, "sub");

        GoogleOAuth2UserService service = stubbedWith(stubbed);

        try {
            service.loadUser(null);
        } catch (OAuth2AuthenticationException ex) {
            assertThat(ex.getError().getErrorCode())
                    .as("o contrato com OAuthFailureHandler exige exatamente 'email_unverified'")
                    .isEqualTo("email_unverified");
            return;
        }
        throw new AssertionError("expected OAuth2AuthenticationException");
    }

    @Test
    void rejeitaEmailBlank() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "blank");
        attrs.put("email", "");
        attrs.put("email_verified", Boolean.TRUE);

        LoginAccountQuery loginAccountQuery = mock(LoginAccountQuery.class);
        GoogleOAuth2UserService service = new GoogleOAuth2UserService(loginAccountQuery) {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) {
                Map<String, Object> a = attrs;
                if (!Boolean.TRUE.equals(a.get("email_verified"))) {
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error("email_unverified",
                                    "E-mail Google nao verificado", null));
                }

                String rawEmail = (String) a.get("email");
                if (rawEmail == null || rawEmail.isBlank()) {
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error("missing_email",
                                    "E-mail Google nao fornecido", null));
                }

                String email = rawEmail.trim().toLowerCase(java.util.Locale.ROOT);
                // Novo contrato: pending signup em vez de excecao
                return loginAccountQuery.findForLogin(email)
                        .map(account -> (OAuth2User) new dev.zayt.atrilha.auth.login.AtrilhaOAuth2User(account, a))
                        .orElseGet(() -> (OAuth2User) dev.zayt.atrilha.auth.login.AtrilhaOAuth2User.pendingSignup(email, a));
            }
        };

        assertThatThrownBy(() -> service.loadUser(null))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .matches(ex -> ((OAuth2AuthenticationException) ex)
                        .getError().getErrorCode().equals("missing_email"));
    }

    @Test
    void normalizaEmailParaLowercase() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "norm");
        attrs.put("email", "  Julia@Example.COM  ");
        attrs.put("email_verified", Boolean.TRUE);

        LoginAccountQuery loginAccountQuery = mock(LoginAccountQuery.class);
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "julia@example.com", null, AccountRole.TEEN, false, "Julia");
        org.mockito.Mockito.when(loginAccountQuery.findForLogin("julia@example.com")).thenReturn(Optional.of(account));

        GoogleOAuth2UserService service = new GoogleOAuth2UserService(loginAccountQuery) {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) {
                Map<String, Object> a = attrs;
                if (!Boolean.TRUE.equals(a.get("email_verified"))) {
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error("email_unverified",
                                    "E-mail Google nao verificado", null));
                }

                String rawEmail = (String) a.get("email");
                if (rawEmail == null || rawEmail.isBlank()) {
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error("missing_email",
                                    "E-mail Google nao fornecido", null));
                }

                String email = rawEmail.trim().toLowerCase(java.util.Locale.ROOT);
                return loginAccountQuery.findForLogin(email)
                        .map(account -> (OAuth2User) new dev.zayt.atrilha.auth.login.AtrilhaOAuth2User(account, a))
                        .orElseGet(() -> (OAuth2User) dev.zayt.atrilha.auth.login.AtrilhaOAuth2User.pendingSignup(email, a));
            }
        };

        OAuth2User result = service.loadUser(null);
        assertThat(result).isInstanceOf(dev.zayt.atrilha.auth.login.AtrilhaOAuth2User.class);
        org.mockito.Mockito.verify(loginAccountQuery).findForLogin("julia@example.com");
    }
}
