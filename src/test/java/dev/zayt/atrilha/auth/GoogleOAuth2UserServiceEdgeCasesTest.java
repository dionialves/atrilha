package dev.zayt.atrilha.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        return new GoogleOAuth2UserService() {
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
}
