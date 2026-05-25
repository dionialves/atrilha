package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.Account;
import dev.zayt.atrilha.accounts.AccountTestFactory;
import dev.zayt.atrilha.auth.OAuth2GoogleSignupChainITStubs;
import dev.zayt.atrilha.auth.OAuth2GoogleSignupChainITStubs.StubGoogleOAuth2UserService;
import dev.zayt.atrilha.auth.PendingGoogleSignup;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes end-to-end da cadeia OAuth2 Google (US-002 / FIX-015, issue #78).
 *
 * <p>Exercita o caminho completo:
 * <pre>
 *   GET /login/oauth2/code/google?code=...&amp;state=...
 *     -> OAuth2LoginAuthenticationFilter (resolve OAuth2AuthorizationRequest da sessao)
 *     -> OAuth2LoginAuthenticationProvider (chama stub do AccessTokenResponseClient)
 *     -> Stub do GoogleOAuth2UserService (resolve principal)
 *     -> OAuthDispatcherSuccessHandler (decide login vs cadastro novo)
 *     -> destino final
 * </pre></p>
 *
 * <p><b>Por que essa abordagem:</b> o {@code OAuth2LoginAuthenticationFilter}
 * exige que a sessao MockMvc contenha um {@link OAuth2AuthorizationRequest}
 * cujo {@code state} bata com o parametro {@code state} da URL — caso contrario
 * lanca {@code authorization_request_not_found} antes mesmo de invocar o
 * userInfoEndpoint. O teste reproduz esse setup manualmente (a request original
 * normalmente seria feita pelo {@code OAuth2AuthorizationRequestRedirectFilter}
 * em {@code /oauth2/authorization/google}, mas pulamos esse passo para exercitar
 * o callback de forma isolada).</p>
 *
 * <p><b>Por que sem OIDC:</b> a request real do Google inclui scope
 * {@code openid}, que rotearia via {@code OidcAuthorizationCodeAuthenticationProvider}
 * — esse provider exige id_token JWT valido, JWK validation, etc. Para o IT,
 * registramos a authorization request com scopes {@code email + profile} (sem
 * openid), o que rota via {@code OAuth2LoginAuthenticationProvider} — que
 * chama o {@code OAuth2AccessTokenResponseClient} stubado e em seguida o
 * {@code OAuth2UserService} stubado. A logica do success/failure handler que
 * estamos validando e identica nos dois caminhos.</p>
 *
 * <p><b>Por que Testcontainers Postgres:</b> os cenarios de login (CA2, CA3)
 * precisam que {@link Account} persistido em {@link EntityManager} seja
 * visivel para {@code JpaLoginAccountQuery}. Reusamos o mesmo padrao do
 * {@code OAuthHandlersIT}. H2 em modo PostgreSQL nao roda as migrations
 * Flyway (sintaxe especifica), entao Testcontainers e a unica forma honesta
 * de testar o caminho real.</p>
 */
@Testcontainers
@SpringBootTest(classes = AtrilhaApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false",
                // FIX-015: usa JpaLoginAccountQuery para que contas persistidas
                // via EntityManager sejam visiveis ao GoogleOAuth2UserService.
                "atrilha.auth.seed.enabled=false"
        })
@Import(OAuth2GoogleSignupChainITStubs.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OAuth2GoogleSignupChainIT {

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

    private static final String SESSION_ATTR_AUTH_REQUEST =
            HttpSessionOAuth2AuthorizationRequestRepository.class.getName() + ".AUTHORIZATION_REQUEST";
    private static final String STATE = "test-state";
    private static final String CODE = "test-code";

    @Autowired
    WebApplicationContext ctx;

    @Autowired
    StubGoogleOAuth2UserService stubService;

    @Autowired
    EntityManager em;

    @Autowired
    PlatformTransactionManager txManager;

    private MockMvc mvc;
    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
        tx = new TransactionTemplate(txManager);
        stubService.reset();
    }

    @AfterEach
    void tearDown() {
        stubService.reset();
    }

    // ----------------------------------------------------------------------
    // CA1: Cadastro novo via Google
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("CA1 — cadastro novo via Google redireciona para /complementar com pendingGoogleSignup na sessao")
    void cadastroNovoViaGoogleRedirecionaParaComplementarComPendingSession() throws Exception {
        stubService.setAttributes(Map.of(
                "sub", "new-user-1",
                "email", "julia-nova@gmail.com",
                "email_verified", Boolean.TRUE,
                "given_name", "Julia"));

        MockHttpSession session = sessionWithAuthRequest();

        mvc.perform(get("/login/oauth2/code/google")
                        .param(OAuth2ParameterNames.CODE, CODE)
                        .param(OAuth2ParameterNames.STATE, STATE)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/adolescente/complementar"));

        Object pending = session.getAttribute("pendingGoogleSignup");
        assertThat(pending)
                .as("CA1: OAuthSuccessHandler deve gravar pendingGoogleSignup na sessao")
                .isInstanceOf(PendingGoogleSignup.class);
        assertThat(((PendingGoogleSignup) pending).email()).isEqualTo("julia-nova@gmail.com");
    }

    // ----------------------------------------------------------------------
    // CA2: Login Google TEEN
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("CA2 — login Google de conta TEEN existente redireciona para /trilha")
    void loginGoogleComContaTeenRedirecionaParaTrilha() throws Exception {
        tx.executeWithoutResult(status ->
                em.persist(AccountTestFactory.newAdolescent("teen-existente@gmail.com")));

        stubService.setAttributes(Map.of(
                "sub", "teen-existing",
                "email", "teen-existente@gmail.com",
                "email_verified", Boolean.TRUE,
                "given_name", "Julia"));

        MockHttpSession session = sessionWithAuthRequest();

        mvc.perform(get("/login/oauth2/code/google")
                        .param(OAuth2ParameterNames.CODE, CODE)
                        .param(OAuth2ParameterNames.STATE, STATE)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trilha"));

        assertThat(session.getAttribute("pendingGoogleSignup"))
                .as("CA2: login real nao deve gravar pendingGoogleSignup")
                .isNull();
    }

    // ----------------------------------------------------------------------
    // CA3: Login Google GUARDIAN sem vinculo
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("CA3 — login Google de conta GUARDIAN sem vinculo redireciona para /vincular")
    void loginGoogleComContaGuardianSemVinculoRedirecionaParaVincular() throws Exception {
        tx.executeWithoutResult(status ->
                em.persist(newGuardianAccount("guardian-sem-vinculo@gmail.com")));

        stubService.setAttributes(Map.of(
                "sub", "guardian-no-link",
                "email", "guardian-sem-vinculo@gmail.com",
                "email_verified", Boolean.TRUE,
                "given_name", "Maria"));

        MockHttpSession session = sessionWithAuthRequest();

        mvc.perform(get("/login/oauth2/code/google")
                        .param(OAuth2ParameterNames.CODE, CODE)
                        .param(OAuth2ParameterNames.STATE, STATE)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vincular"));
    }

    // ----------------------------------------------------------------------
    // CA3: Login Google GUARDIAN com vinculo
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("CA3 — login Google de conta GUARDIAN com vinculo redireciona para /painel")
    void loginGoogleComContaGuardianComVinculoRedirecionaParaPainel() throws Exception {
        tx.executeWithoutResult(status ->
                em.persist(newGuardianAccount("guardian-com-vinculo@gmail.com")));

        // JpaLoginAccountQuery hardcodes hasGuardianLink=false (Sprint 3 — tabela
        // de vinculo so chega em US-014). Override via stub permite testar o branch.
        stubService.setHasGuardianLinkOverride("guardian-com-vinculo@gmail.com", true);

        stubService.setAttributes(Map.of(
                "sub", "guardian-with-link",
                "email", "guardian-com-vinculo@gmail.com",
                "email_verified", Boolean.TRUE,
                "given_name", "Maria"));

        MockHttpSession session = sessionWithAuthRequest();

        mvc.perform(get("/login/oauth2/code/google")
                        .param(OAuth2ParameterNames.CODE, CODE)
                        .param(OAuth2ParameterNames.STATE, STATE)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/painel"));
    }

    // ----------------------------------------------------------------------
    // CA4: E-mail nao verificado
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("CA4 — email_verified=false redireciona para escolher-metodo?error=email_unverified")
    void emailGoogleNaoVerificadoRedirecionaComEmailUnverified() throws Exception {
        stubService.setAttributes(Map.of(
                "sub", "unverified",
                "email", "nao-verificado@gmail.com",
                "email_verified", Boolean.FALSE,
                "given_name", "Julia"));

        MockHttpSession session = sessionWithAuthRequest();

        mvc.perform(get("/login/oauth2/code/google")
                        .param(OAuth2ParameterNames.CODE, CODE)
                        .param(OAuth2ParameterNames.STATE, STATE)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/adolescente/escolher-metodo?error=email_unverified"));
    }

    // ----------------------------------------------------------------------
    // CA5: Cancelamento no Google
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("CA5 — cancelamento no Google redireciona para escolher-metodo?error=cancelled")
    void cancelamentoNoGoogleRedirecionaComCancelled() throws Exception {
        stubService.setCancelException(true);

        MockHttpSession session = sessionWithAuthRequest();

        mvc.perform(get("/login/oauth2/code/google")
                        .param(OAuth2ParameterNames.CODE, CODE)
                        .param(OAuth2ParameterNames.STATE, STATE)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/adolescente/escolher-metodo?error=cancelled"));
    }

    // ----------------------------------------------------------------------
    // CA7: Normalizacao de email em case misto
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("CA7 — email Google em case misto normaliza para lowercase em pendingGoogleSignup")
    void cadastroNovoComEmailEmCaseMistoNormalizaParaLowercase() throws Exception {
        stubService.setAttributes(Map.of(
                "sub", "case-misto",
                "email", "Julia@Example.COM ",
                "email_verified", Boolean.TRUE,
                "given_name", "Julia"));

        MockHttpSession session = sessionWithAuthRequest();

        mvc.perform(get("/login/oauth2/code/google")
                        .param(OAuth2ParameterNames.CODE, CODE)
                        .param(OAuth2ParameterNames.STATE, STATE)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/adolescente/complementar"));

        assertThat(stubService.getLastPendingEmail()).isEqualTo("julia@example.com");

        PendingGoogleSignup pending =
                (PendingGoogleSignup) session.getAttribute("pendingGoogleSignup");
        assertThat(pending)
                .as("CA7: pendingGoogleSignup deve ter email normalizado")
                .isNotNull();
        assertThat(pending.email()).isEqualTo("julia@example.com");
    }

    // ----------------------------------------------------------------------
    // CA8: Dispatcher encaminha pending signup para OAuthSuccessHandler
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("CA8 — dispatcher encaminha principal PENDING_SIGNUP para OAuthSuccessHandler")
    void dispatcherEncaminhaPendingSignupParaOAuthSuccessHandler() throws Exception {
        stubService.setAttributes(Map.of(
                "sub", "dispatcher-test",
                "email", "dispatcher@test.com",
                "email_verified", Boolean.TRUE,
                "given_name", "Test"));

        MockHttpSession session = sessionWithAuthRequest();

        mvc.perform(get("/login/oauth2/code/google")
                        .param(OAuth2ParameterNames.CODE, CODE)
                        .param(OAuth2ParameterNames.STATE, STATE)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/adolescente/complementar"));

        assertThat(stubService.getLastPendingEmail()).isEqualTo("dispatcher@test.com");
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    /**
     * Cria uma sessao MockMvc com um {@link OAuth2AuthorizationRequest} pre-populado
     * em {@code HttpSessionOAuth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST}.
     *
     * <p>O {@code OAuth2LoginAuthenticationFilter} le esse atributo para validar
     * o {@code state} do callback. Sem essa pre-populacao, o filter lanca
     * {@code authorization_request_not_found}.</p>
     *
     * <p>Scopes sao {@code email + profile} (sem {@code openid}) propositalmente
     * para evitar a chain OIDC, que requer JWT id_token valido. Ver Javadoc da
     * classe.</p>
     */
    private static MockHttpSession sessionWithAuthRequest() {
        OAuth2AuthorizationRequest authRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("test-client-id")
                .redirectUri("http://localhost/login/oauth2/code/google")
                .scopes(Set.of("email", "profile"))
                .state(STATE)
                .attributes(attrs -> attrs.put(OAuth2ParameterNames.REGISTRATION_ID, "google"))
                .build();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_ATTR_AUTH_REQUEST, authRequest);
        return session;
    }

    private static Account newGuardianAccount(String email) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setType("GUARDIAN");
        a.setEmail(email);
        a.setPasswordHash("$2b$12$" + "a".repeat(53));
        a.setCreatedAt(OffsetDateTime.now());
        return a;
    }
}
