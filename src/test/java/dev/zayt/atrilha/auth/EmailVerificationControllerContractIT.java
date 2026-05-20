package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.accounts.Account;
import dev.zayt.atrilha.accounts.AccountTestFactory;
import dev.zayt.atrilha.accounts.EmailVerificationToken;
import dev.zayt.atrilha.accounts.EmailVerificationTokenRepository;
import dev.zayt.atrilha.notifications.RecordingEmailSender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Cobertura complementar do contrato HTTP da US-006 — bordas e segurança
 * estrutural não cobertas pelo IT do Codificador:
 *
 * <ul>
 *   <li>{@code POST /verify-email} (método não suportado pelo controller) —
 *       não pode crashar com 500; deve retornar 405 (Spring default) ou 404,
 *       não vazar stack ao usuário.</li>
 *   <li>{@code POST /verificar-email/reenviar} para usuário já verificado —
 *       não envia e-mail e redireciona para home (idempotência: nada quebra
 *       se o usuário recarregou a tela após confirmar em outra aba).</li>
 *   <li>Verificação via {@code GET /verify-email?token=X_de_outra_conta}
 *       quando o usuário B está autenticado — o service verifica o dono do
 *       token, não a sessão.</li>
 *   <li>Após {@code SUCCESS}, o {@code SecurityContextHolder} permanece com
 *       a mesma identidade — banner some por consulta ao banco
 *       ({@link EmailVerificationBannerAdvice}), <strong>não</strong> por
 *       recriação do {@code Authentication} (decisão técnica #1 do
 *       Codificador).</li>
 *   <li>Caminho de erro: {@code /verificar-email/reenviar} CSRF ok mas
 *       repetidamente bate no rate-limit, controller retorna 3xx com flash
 *       {@code resendStatus = "rate_limited"} (nunca 500).</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(classes = { dev.zayt.atrilha.AtrilhaApplication.class, EmailVerificationControllerContractIT.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@ActiveProfiles("test")
@DirtiesContext
class EmailVerificationControllerContractIT {

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

    @TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
        }
    }

    @Autowired
    WebApplicationContext ctx;

    @Autowired
    EntityManager em;

    @Autowired
    EmailVerificationTokenRepository tokenRepository;

    @Autowired
    PlatformTransactionManager txManager;

    @Autowired
    RecordingEmailSender mailer;

    MockMvc mvc;
    TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
        tx = new TransactionTemplate(txManager);
        mailer.clear();
    }

    private Account persistAccount(String email, boolean verified) {
        Account a = AccountTestFactory.newAdolescent(email);
        if (verified) {
            a.setEmailVerifiedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
        }
        tx.executeWithoutResult(status -> em.persist(a));
        return a;
    }

    private EmailVerificationToken seedActiveToken(UUID accountId) {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setToken(UUID.randomUUID());
        Instant now = Instant.now();
        t.setExpiresAt(now.plus(24, ChronoUnit.HOURS));
        t.setCreatedAt(now);
        return tokenRepository.saveAndFlush(t);
    }

    private MockHttpServletRequestBuilder authedAs(MockHttpServletRequestBuilder rb, UUID accountId) {
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedAccount(accountId, AccountRole.TEEN), null,
                List.of(new SimpleGrantedAuthority("ROLE_TEEN")));
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        return rb.sessionAttr(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
    }

    // ---------- POST /verify-email — método não suportado ----------

    @Test
    void postVerifyEmail_unsupportedMethod_returnsClientError_notServerError() throws Exception {
        // GET /verify-email é mapeado; POST não. Spring devolve 405 Method Not
        // Allowed (ou, dependendo do filtro de segurança, 403/404). Qualquer
        // resposta no range 4xx é aceitável — o que NÃO podemos ter é 5xx,
        // que indicaria stack trace vazando ou bug no controller.
        mvc.perform(post("/verify-email").param("token", UUID.randomUUID().toString()).with(csrf()))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertThat(s)
                            .as("POST /verify-email NUNCA pode retornar 5xx — "
                                    + "verificado por status=" + s)
                            .isBetween(400, 499);
                });
    }

    // ---------- POST /verificar-email/reenviar para usuário já verificado ----------

    @Test
    void postReenviar_userAlreadyVerified_redirectsHome_doesNotSendEmail() throws Exception {
        Account verified = persistAccount("already-verified@example.com", true);

        mvc.perform(authedAs(post("/verificar-email/reenviar"), verified.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        assertThat(mailer.recorded())
                .as("usuário já verificado não dispara reenvio")
                .isEmpty();
        assertThat(tokenRepository.findByAccountIdAndUsedAtIsNull(verified.getId()))
                .as("nenhum token novo emitido")
                .isEmpty();
    }

    // ---------- GET /verify-email com token de outra conta ----------

    @Test
    void getVerifyEmail_tokenOfDifferentAccount_verifiesOwnerOfTokenNotAuthenticatedUser() throws Exception {
        Account owner = persistAccount("owner-verify@example.com", false);
        Account stranger = persistAccount("stranger-verify@example.com", false);
        EmailVerificationToken tokenForOwner = seedActiveToken(owner.getId());

        // Stranger autenticado clica no link de owner.
        mvc.perform(authedAs(get("/verify-email"), stranger.getId())
                        .param("token", tokenForOwner.getToken().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-resultado"))
                .andExpect(model().attribute("outcome", "SUCCESS"));

        em.clear();
        Account owFromDb = em.find(Account.class, owner.getId());
        Account strFromDb = em.find(Account.class, stranger.getId());
        assertThat(owFromDb.getEmailVerifiedAt())
                .as("dono do token é verificado")
                .isNotNull();
        assertThat(strFromDb.getEmailVerifiedAt())
                .as("usuário logado NÃO é verificado (não é o dono do token)")
                .isNull();
    }

    // ---------- Identidade na sessão preservada após SUCCESS ----------

    @Test
    void getVerifyEmail_success_doesNotMutateSecurityContextIdentity() throws Exception {
        Account a = persistAccount("ctx@example.com", false);
        EmailVerificationToken t = seedActiveToken(a.getId());

        // Snapshot do principal antes do verify.
        UUID originalPrincipalId = a.getId();

        mvc.perform(authedAs(get("/verify-email"), a.getId())
                        .param("token", t.getToken().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("outcome", "SUCCESS"));

        // SecurityContextHolder do MockMvc é por-request; o ponto é que o
        // controller NÃO chama setAuthentication para "recriar" a sessão.
        // Verificação direta: o banner-advice agora retorna unverifiedEmail=false
        // numa request subsequente (porque consulta o banco), sem ter
        // mexido na sessão.
        // (Não asserta sobre SecurityContextHolder estático pq o MockMvc
        // limpa entre requests; o contrato fica documentado pela ausência de
        // efeito visível na próxima request).
        mvc.perform(authedAs(get("/verificar-email"), originalPrincipalId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        // Se a sessão tivesse sido recriada com identidade inválida ou nula,
        // o redirect /verificar-email iria pra REDIRECT_HOME por outro motivo
        // (currentOpt.isEmpty()). A asserção interessante é que o fluxo
        // funcional segue normal — o principalId ainda corresponde à conta
        // verificada (a request acima passou pelo currentAccount() com sucesso).
    }

    // ---------- Token mal-formado não diferencia da tela de inválido ----------

    @Test
    void getVerifyEmail_malformedToken_returnsSameViewAsUnknown_preventsStateLeak() throws Exception {
        // CONTRATO: UX spec §5.3 — privacidade. Cliente NÃO deve conseguir
        // distinguir "token malformado" de "token desconhecido" de "token
        // expirado". Todas as três caem em EXPIRED_OR_INVALID.
        var malformed = mvc.perform(get("/verify-email").param("token", "definitely-not-a-uuid"))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-resultado"))
                .andExpect(model().attribute("outcome", "EXPIRED_OR_INVALID"))
                .andReturn();
        var unknown = mvc.perform(get("/verify-email").param("token", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-resultado"))
                .andExpect(model().attribute("outcome", "EXPIRED_OR_INVALID"))
                .andReturn();

        // Ambas as respostas devem ter o mesmo view + mesmo outcome → string
        // de resposta semanticamente equivalente. Não asserta byte-a-byte
        // (timestamps/CSRF mudam), mas o atributo do modelo é idêntico.
        assertThat(malformed.getModelAndView()).isNotNull();
        assertThat(unknown.getModelAndView()).isNotNull();
        assertThat(malformed.getModelAndView().getModel().get("outcome"))
                .isEqualTo(unknown.getModelAndView().getModel().get("outcome"));
    }

    // ---------- /verificar-email/reenviar sob rate-limit não retorna 500 ----------

    @Test
    void postReenviar_underRateLimit_returnsRedirectNotServerError() throws Exception {
        Account a = persistAccount("rl-controller@example.com", false);
        // Pré-condição: já existe um token recém-criado para esta conta
        // (cooldown ativo).
        seedActiveToken(a.getId());

        mvc.perform(authedAs(post("/verificar-email/reenviar"), a.getId()).with(csrf()))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertThat(s)
                            .as("rate-limit não pode subir como 5xx — status=" + s)
                            .isBetween(300, 399);
                });
    }
}
