package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.AccountTestFactory;
import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

/**
 * Integracao dos {@code OAuthSuccessHandler} / {@code OAuthFailureHandler}
 * (US-002 / Issue #37).
 *
 * <p>Usa Postgres real porque o success handler consulta
 * {@link dev.zayt.atrilha.accounts.AccountReader#existsByEmailIgnoreCase}.
 * Para fabricar contas existentes, usa {@link AccountTestFactory} +
 * {@link EntityManager} (caminho ja estabelecido pelo
 * {@code AccountRegisteredEventListenerIT}, que tambem nao pode chamar o
 * package-private {@code RegisterAdolescentService}).</p>
 */
@Testcontainers
@SpringBootTest(classes = AtrilhaApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class OAuthHandlersIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("atrilha")
                    .withUsername("atrilha")
                    .withPassword("atrilha");

    @DynamicPropertySource
    static void registerPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    OAuthSuccessHandler successHandler;

    @Autowired
    OAuthFailureHandler failureHandler;

    @Autowired
    PlatformTransactionManager txManager;

    @Autowired
    EntityManager em;

    TransactionTemplate tx;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        tx = new TransactionTemplate(txManager);
    }

    private static OAuth2AuthenticationToken googleToken(String email) {
        OAuth2User user = new DefaultOAuth2User(
                createAuthorityList("OAUTH2_USER"),
                Map.of(
                        "sub", "12345",
                        "email", email,
                        "email_verified", true,
                        "given_name", "Julia",
                        "picture", "https://lh3.googleusercontent.com/a/julia"),
                "sub");
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");
    }

    // 22
    @Test
    void successHandlerContaExistenteRedirecionaComAccountExists() throws Exception {
        // Pre-cria conta com mesmo e-mail.
        tx.executeWithoutResult(status ->
                em.persist(AccountTestFactory.newAdolescent("existe@gmail.com")));

        MockHttpServletRequest req = new MockHttpServletRequest("GET",
                "/login/oauth2/code/google");
        MockHttpServletResponse res = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(req, res, googleToken("existe@gmail.com"));

        assertThat(res.getStatus()).isEqualTo(302);
        assertThat(res.getRedirectedUrl())
                .isEqualTo("/cadastro/adolescente/escolher-metodo?error=account_exists");
        // SecurityContext esvaziado (Julia nao deve ficar autenticada como ela mesma
        // antes de logar — esta US so cobre cadastro novo).
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // pendingGoogleSignup nao deve ter sido posto na sessao.
        Object sessionAttr = req.getSession().getAttribute("pendingGoogleSignup");
        assertThat(sessionAttr).isNull();
    }

    // 23
    @Test
    void successHandlerNovaContaGravaPendingERedirecionaComplementar() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET",
                "/login/oauth2/code/google");
        MockHttpServletResponse res = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(req, res, googleToken("nova@gmail.com"));

        assertThat(res.getStatus()).isEqualTo(302);
        assertThat(res.getRedirectedUrl()).isEqualTo("/cadastro/adolescente/complementar");

        Object pending = req.getSession().getAttribute("pendingGoogleSignup");
        assertThat(pending).isInstanceOf(PendingGoogleSignup.class);
        PendingGoogleSignup p = (PendingGoogleSignup) pending;
        assertThat(p.email()).isEqualTo("nova@gmail.com");
        assertThat(p.givenName()).isEqualTo("Julia");
        assertThat(p.picture()).isEqualTo("https://lh3.googleusercontent.com/a/julia");
        assertThat(p.emailVerifiedAt()).isNotNull();
        assertThat(p.createdAt()).isNotNull();
    }

    @Test
    void successHandlerNormalizaEmailLowercase() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET",
                "/login/oauth2/code/google");
        MockHttpServletResponse res = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(req, res, googleToken("Nova.Case@Gmail.com"));

        PendingGoogleSignup p = (PendingGoogleSignup)
                req.getSession().getAttribute("pendingGoogleSignup");
        assertThat(p.email()).isEqualTo("nova.case@gmail.com");
    }

    // 24
    @Test
    void failureHandlerAccessDeniedRedirecionaCancelled() throws Exception {
        AuthenticationException ex = new OAuth2AuthenticationException(
                new OAuth2Error("access_denied", "user cancelled", null));
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(req, res, ex);

        assertThat(res.getStatus()).isEqualTo(302);
        assertThat(res.getRedirectedUrl())
                .isEqualTo("/cadastro/adolescente/escolher-metodo?error=cancelled");
    }

    @Test
    void failureHandlerUserCancelledLoginRedirecionaCancelled() throws Exception {
        AuthenticationException ex = new OAuth2AuthenticationException(
                new OAuth2Error("user_cancelled_login", "user closed", null));
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(req, res, ex);

        assertThat(res.getRedirectedUrl())
                .isEqualTo("/cadastro/adolescente/escolher-metodo?error=cancelled");
    }

    // 25
    @Test
    void failureHandlerEmailUnverifiedRedirecionaEmailUnverified() throws Exception {
        AuthenticationException ex = new OAuth2AuthenticationException(
                new OAuth2Error("email_unverified", "Google ainda nao confirmou", null));
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(req, res, ex);

        assertThat(res.getStatus()).isEqualTo(302);
        assertThat(res.getRedirectedUrl())
                .isEqualTo("/cadastro/adolescente/escolher-metodo?error=email_unverified");
    }

    // 26
    @Test
    void failureHandlerOutrosErrosRedirecionamGenerico() throws Exception {
        AuthenticationException ex = new OAuth2AuthenticationException(
                new OAuth2Error("server_error", "boom", null));
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(req, res, ex);

        assertThat(res.getRedirectedUrl())
                .isEqualTo("/cadastro/adolescente/escolher-metodo?error=oauth");
    }

    @Test
    void failureHandlerNaoOauthExceptionRedirecionaGenerico() throws Exception {
        AuthenticationException ex = new AuthenticationException("foo") {};
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(req, res, ex);

        assertThat(res.getRedirectedUrl())
                .isEqualTo("/cadastro/adolescente/escolher-metodo?error=oauth");
    }

    @Test
    void successHandlerComAuthenticationDeOutroTipoCaiNoFluxoGenerico() {
        // O success handler espera OAuth2AuthenticationToken. Para defesa em
        // profundidade, qualquer outro Authentication deve disparar excecao
        // — esse contrato impede silenciosamente "logar como TEEN" via outro
        // provedor antes que a logica esteja pronta.
        var other = new TestingAuthenticationToken("foo", "bar");
        org.junit.jupiter.api.Assertions.assertThrows(ClassCastException.class, () ->
                successHandler.onAuthenticationSuccess(
                        new MockHttpServletRequest(),
                        new MockHttpServletResponse(),
                        other));
    }
}
