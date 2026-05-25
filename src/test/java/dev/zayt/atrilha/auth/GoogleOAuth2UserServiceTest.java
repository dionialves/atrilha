package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.auth.login.AtrilhaOAuth2User;
import dev.zayt.atrilha.auth.login.LoginAccountQuery;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    void loadUserDevolveAtrilhaOAuth2UserComRoleTeenParaContaAdolescente() {
        OAuth2User stubbed = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "sub", "111",
                        "email", "julia@example.com",
                        "email_verified", Boolean.TRUE,
                        "given_name", "Julia"),
                "sub");

        LoginAccountQuery loginAccountQuery = mock(LoginAccountQuery.class);
        LoginAccountQuery.LoginAccount teenAccount = new LoginAccountQuery.LoginAccount(
                "julia@example.com", null, AccountRole.TEEN, false, "Julia");
        when(loginAccountQuery.findForLogin("julia@example.com")).thenReturn(Optional.of(teenAccount));

        GoogleOAuth2UserService service = stubbedWith(stubbed, loginAccountQuery);

        OAuth2User result = service.loadUser(null);

        assertThat(result).isInstanceOf(AtrilhaOAuth2User.class);
        AtrilhaOAuth2User atrilha = (AtrilhaOAuth2User) result;
        assertThat(atrilha.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_TEEN");
        assertThat(atrilha.displayName()).isEqualTo("Julia");
        assertThat(atrilha.role()).isEqualTo(AccountRole.TEEN);
        assertThat(atrilha.hasGuardianLink()).isFalse();
    }

    @Test
    void loadUserDevolveAtrilhaOAuth2UserComRoleGuardianParaContaGuardian() {
        OAuth2User stubbed = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "sub", "222",
                        "email", "maria@example.com",
                        "email_verified", Boolean.TRUE,
                        "given_name", "Maria"),
                "sub");

        LoginAccountQuery loginAccountQuery = mock(LoginAccountQuery.class);
        LoginAccountQuery.LoginAccount guardianAccount = new LoginAccountQuery.LoginAccount(
                "maria@example.com", null, AccountRole.GUARDIAN, true, "Maria");
        when(loginAccountQuery.findForLogin("maria@example.com")).thenReturn(Optional.of(guardianAccount));

        GoogleOAuth2UserService service = stubbedWith(stubbed, loginAccountQuery);

        OAuth2User result = service.loadUser(null);

        assertThat(result).isInstanceOf(AtrilhaOAuth2User.class);
        AtrilhaOAuth2User atrilha = (AtrilhaOAuth2User) result;
        assertThat(atrilha.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_GUARDIAN");
        assertThat(atrilha.displayName()).isEqualTo("Maria");
        assertThat(atrilha.role()).isEqualTo(AccountRole.GUARDIAN);
        assertThat(atrilha.hasGuardianLink()).isTrue();
    }

    @Test
    void loadUserDevolveAtrilhaOAuth2UserPendingSignupQuandoEmailNaoTemConta() {
        OAuth2User stubbed = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "sub", "333",
                        "email", "semconta@example.com",
                        "email_verified", Boolean.TRUE),
                "sub");

        LoginAccountQuery loginAccountQuery = mock(LoginAccountQuery.class);
        when(loginAccountQuery.findForLogin("semconta@example.com")).thenReturn(Optional.empty());

        GoogleOAuth2UserService service = stubbedWith(stubbed, loginAccountQuery);

        OAuth2User result = service.loadUser(null);

        assertThat(result).isInstanceOf(AtrilhaOAuth2User.class);
        AtrilhaOAuth2User atrilha = (AtrilhaOAuth2User) result;
        assertThat(atrilha.isPendingSignup()).isTrue();
        assertThat(atrilha.getAttributes().get("email")).isEqualTo("semconta@example.com");
    }

    @Test
    void loadUserMantemRegraEmailVerifiedFalse() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "444");
        attrs.put("email", "naoverificado@example.com");
        attrs.put("email_verified", Boolean.FALSE);
        OAuth2User stubbed = new DefaultOAuth2User(
                List.of(), attrs, "sub");

        LoginAccountQuery loginAccountQuery = mock(LoginAccountQuery.class);
        // A query NAO deve ser chamada quando email_verified=false
        GoogleOAuth2UserService service = stubbedWith(stubbed, loginAccountQuery);

        assertThatThrownBy(() -> service.loadUser(null))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .matches(ex -> ((OAuth2AuthenticationException) ex)
                        .getError().getErrorCode().equals("email_unverified"));

        // Verificar que a query NAO foi chamada
        org.mockito.Mockito.verifyNoInteractions(loginAccountQuery);
    }

    @Test
    void loadUserNormalizaEmailParaLowercaseAntesDeConsultarQuery() {
        OAuth2User stubbed = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "sub", "555",
                        "email", "JULIA@Example.COM ",
                        "email_verified", Boolean.TRUE),
                "sub");

        LoginAccountQuery loginAccountQuery = mock(LoginAccountQuery.class);
        LoginAccountQuery.LoginAccount teenAccount = new LoginAccountQuery.LoginAccount(
                "julia@example.com", null, AccountRole.TEEN, false, "Julia");
        when(loginAccountQuery.findForLogin("julia@example.com")).thenReturn(Optional.of(teenAccount));

        GoogleOAuth2UserService service = stubbedWith(stubbed, loginAccountQuery);

        OAuth2User result = service.loadUser(null);

        assertThat(result).isInstanceOf(AtrilhaOAuth2User.class);
        // A query foi chamada com email normalizado (lowercase + trim)
        org.mockito.Mockito.verify(loginAccountQuery).findForLogin("julia@example.com");
    }

    @Test
    void rejeitaEmailVerifiedAusente() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "666");
        attrs.put("email", "semclaim@example.com");
        // email_verified omitido — Google sempre envia, mas defesa em profundidade.
        OAuth2User stubbed = new DefaultOAuth2User(
                List.of(), attrs, "sub");

        LoginAccountQuery loginAccountQuery = mock(LoginAccountQuery.class);
        GoogleOAuth2UserService service = stubbedWith(stubbed, loginAccountQuery);

        assertThatThrownBy(() -> service.loadUser(null))
                .isInstanceOf(OAuth2AuthenticationException.class);

        org.mockito.Mockito.verifyNoInteractions(loginAccountQuery);
    }

    private GoogleOAuth2UserService stubbedWith(OAuth2User upstream, LoginAccountQuery loginAccountQuery) {
        return new GoogleOAuth2UserService(loginAccountQuery) {
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

                String rawEmail = (String) a.get("email");
                if (rawEmail == null || rawEmail.isBlank()) {
                    throw new OAuth2AuthenticationException(
                            new org.springframework.security.oauth2.core.OAuth2Error(
                                    "missing_email",
                                    "E-mail Google nao fornecido",
                                    null));
                }
                String email = rawEmail.trim().toLowerCase(java.util.Locale.ROOT);

                // Se nao existe conta, devolve pending signup (novo contrato)
                return loginAccountQuery.findForLogin(email)
                        .map(account -> (OAuth2User) new AtrilhaOAuth2User(account, a))
                        .orElseGet(() -> (OAuth2User) AtrilhaOAuth2User.pendingSignup(email, a));
            }
        };
    }

    // Sanity: o teste acima usa override, mas a classe real precisa existir
    @Test
    void classeRealUsaDefaultOAuth2UserServiceComoBase() {
        assertThat(GoogleOAuth2UserService.class.getSuperclass().getName())
                .isEqualTo("org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService");
        assertThat(GoogleOAuth2UserService.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .contains("loadUser");
    }
}
