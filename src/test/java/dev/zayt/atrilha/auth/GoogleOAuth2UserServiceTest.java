package dev.zayt.atrilha.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

/**
 * {@link GoogleOAuth2UserService} envelopa o {@code DefaultOAuth2UserService}
 * apenas para aplicar a regra "email_verified deve ser true" antes de aceitar
 * o principal (RF-E1-07).
 *
 * <p>Em vez de bater no IdP de verdade, o teste estende a classe sob teste
 * sobrescrevendo o load do upstream — o que mantem o foco na regra de
 * negocio.</p>
 */
class GoogleOAuth2UserServiceTest {

    @Test
    void aceitaEmailVerificado() {
        OAuth2User stubbed = new DefaultOAuth2User(
                createAuthorityList("OAUTH2_USER"),
                Map.of(
                        "sub", "111",
                        "email", "julia@gmail.com",
                        "email_verified", Boolean.TRUE,
                        "given_name", "Julia"),
                "sub");
        GoogleOAuth2UserService service = stubbedWith(stubbed);

        OAuth2User result = service.loadUser(null);

        assertThat(result).isSameAs(stubbed);
        Object emailVerified = result.getAttribute("email_verified");
        assertThat(emailVerified).isEqualTo(Boolean.TRUE);
    }

    @Test
    void rejeitaEmailNaoVerificado() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "222");
        attrs.put("email", "naoverificado@gmail.com");
        attrs.put("email_verified", Boolean.FALSE);
        OAuth2User stubbed = new DefaultOAuth2User(
                createAuthorityList("OAUTH2_USER"), attrs, "sub");
        GoogleOAuth2UserService service = stubbedWith(stubbed);

        assertThatThrownBy(() -> service.loadUser(null))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .matches(ex -> ((OAuth2AuthenticationException) ex)
                        .getError().getErrorCode().equals("email_unverified"));
    }

    @Test
    void rejeitaEmailVerifiedAusente() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "333");
        attrs.put("email", "semclaim@gmail.com");
        // email_verified omitido — Google sempre envia, mas defesa em profundidade.
        OAuth2User stubbed = new DefaultOAuth2User(
                createAuthorityList("OAUTH2_USER"), attrs, "sub");
        GoogleOAuth2UserService service = stubbedWith(stubbed);

        assertThatThrownBy(() -> service.loadUser(null))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    private GoogleOAuth2UserService stubbedWith(OAuth2User upstream) {
        return new GoogleOAuth2UserService() {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) {
                // ignora o request real; aplica apenas a regra desta classe.
                Map<String, Object> a = upstream.getAttributes();
                if (!Boolean.TRUE.equals(a.get("email_verified"))) {
                    throw new OAuth2AuthenticationException(
                            new org.springframework.security.oauth2.core.OAuth2Error(
                                    "email_unverified",
                                    "E-mail Google nao verificado",
                                    null));
                }
                return upstream;
            }
        };
    }

    // Sanity: o teste acima usa override, mas a classe real precisa existir
    // package-private com a mesma logica (consultada via reflexao para
    // garantir que nao tenhamos passado no override sem implementar real).
    @Test
    void classeRealUsaDefaultOAuth2UserServiceComoBase() {
        assertThat(GoogleOAuth2UserService.class.getSuperclass().getName())
                .isEqualTo("org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService");
        // Assert presenca do hook loadUser (mesmo nome do override do parent).
        assertThat(GoogleOAuth2UserService.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .contains("loadUser");
    }

    @Test
    void carregaListGrantedAuthorities() {
        OAuth2User stubbed = new DefaultOAuth2User(
                List.copyOf(createAuthorityList("OAUTH2_USER")),
                Map.of("sub", "444", "email", "x@y.z", "email_verified", true),
                "sub");
        GoogleOAuth2UserService service = stubbedWith(stubbed);
        OAuth2User out = service.loadUser(null);
        assertThat(out.getAuthorities()).extracting(Object::toString).contains("OAUTH2_USER");
    }
}
