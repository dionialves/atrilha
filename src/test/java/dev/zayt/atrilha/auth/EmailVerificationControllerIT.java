package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.accounts.AccountTestFactory;
import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.auth.verification.EmailVerificationToken;
import dev.zayt.atrilha.auth.verification.EmailVerificationTokenRepository;
import dev.zayt.atrilha.auth.verification.EmailVerificationService;
import dev.zayt.atrilha.auth.domain.AuthenticatedAccount;
import dev.zayt.atrilha.accounts.domain.AccountRole;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Controller IT do fluxo de verificação de e-mail (US-006).
 *
 * <p>Cobre os três endpoints introduzidos pela US:
 * <ul>
 *   <li>{@code GET /verificar-email} (autenticado).</li>
 *   <li>{@code POST /verificar-email/reenviar} (autenticado, CSRF).</li>
 *   <li>{@code GET /verify-email?token=...} (público).</li>
 * </ul>
 * </p>
 */
@Testcontainers
@SpringBootTest(classes = { dev.zayt.atrilha.AtrilhaApplication.class, EmailVerificationControllerIT.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@ActiveProfiles("test")
@DirtiesContext
class EmailVerificationControllerIT {

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
            a.setEmailVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
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

    private EmailVerificationToken seedExpiredToken(UUID accountId) {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setToken(UUID.randomUUID());
        Instant past = Instant.now().minus(25, ChronoUnit.HOURS);
        t.setExpiresAt(past.plus(1, ChronoUnit.MINUTES));
        t.setCreatedAt(past);
        return tokenRepository.saveAndFlush(t);
    }

    private EmailVerificationToken seedUsedToken(UUID accountId) {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setToken(UUID.randomUUID());
        Instant now = Instant.now();
        t.setExpiresAt(now.plus(24, ChronoUnit.HOURS));
        t.setCreatedAt(now);
        t.setUsedAt(now);
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

    // ---------- GET /verificar-email ----------

    @Test
    void getVerificarEmail_authenticatedUnverifiedUser_rendersPageWithEmail() throws Exception {
        Account a = persistAccount("pending@example.com", false);

        mvc.perform(authedAs(get("/verificar-email"), a.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("verificar-email"))
                .andExpect(model().attribute("email", "pending@example.com"));
    }

    @Test
    void getVerificarEmail_authenticatedVerifiedUser_redirectsToHome() throws Exception {
        Account a = persistAccount("verified@example.com", true);

        mvc.perform(authedAs(get("/verificar-email"), a.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void getVerificarEmail_anonymous_isUnauthorized() throws Exception {
        // Sem authenticação. Spring Security default → redirecionar para login.
        // Como não há /login configurado, a rota retorna 403 (forbidden) ou 302 com
        // redirect default. Aceitamos qualquer comportamento que indique bloqueio.
        mvc.perform(get("/verificar-email"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(s)
                            .as("anonymous deve receber 3xx/401/403")
                            .satisfiesAnyOf(
                                    code -> org.assertj.core.api.Assertions.assertThat(code).isBetween(300, 399),
                                    code -> org.assertj.core.api.Assertions.assertThat(code).isEqualTo(401),
                                    code -> org.assertj.core.api.Assertions.assertThat(code).isEqualTo(403));
                });
    }

    // ---------- POST /verificar-email/reenviar ----------

    @Test
    void postReenviar_authenticated_withCsrf_redirectsBackWithSuccess() throws Exception {
        Account a = persistAccount("resend@example.com", false);

        mvc.perform(authedAs(post("/verificar-email/reenviar"), a.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verificar-email"))
                .andExpect(flash().attributeExists("resendStatus"));

        // E-mail despachado pelo RecordingEmailSender
        org.assertj.core.api.Assertions.assertThat(mailer.recorded()).hasSize(1);
    }

    @Test
    void postReenviar_withoutCsrf_isForbidden() throws Exception {
        Account a = persistAccount("nocsrf@example.com", false);

        mvc.perform(authedAs(post("/verificar-email/reenviar"), a.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void postReenviar_rateLimited_redirectsWithRateLimitMessage() throws Exception {
        Account a = persistAccount("rl@example.com", false);
        // Pré-condiciona: já houve uma emissão recente (dentro do cooldown)
        seedActiveToken(a.getId());

        mvc.perform(authedAs(post("/verificar-email/reenviar"), a.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verificar-email"))
                .andExpect(flash().attributeExists("resendStatus"));
    }

    // ---------- GET /verify-email?token=... (público) ----------

    @Test
    void getVerifyEmail_validToken_publicAccess_rendersSuccess() throws Exception {
        Account a = persistAccount("verify-ok@example.com", false);
        EmailVerificationToken t = seedActiveToken(a.getId());

        mvc.perform(get("/verify-email").param("token", t.getToken().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-resultado"))
                .andExpect(model().attribute("outcome", "SUCCESS"));
    }

    @Test
    void getVerifyEmail_expiredToken_publicAccess_rendersFailed() throws Exception {
        Account a = persistAccount("verify-expired@example.com", false);
        EmailVerificationToken t = seedExpiredToken(a.getId());

        mvc.perform(get("/verify-email").param("token", t.getToken().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-resultado"))
                .andExpect(model().attribute("outcome", "EXPIRED_OR_INVALID"));
    }

    @Test
    void getVerifyEmail_alreadyUsedToken_publicAccess_rendersAlreadyUsed() throws Exception {
        Account a = persistAccount("verify-used@example.com", true);
        EmailVerificationToken t = seedUsedToken(a.getId());

        mvc.perform(get("/verify-email").param("token", t.getToken().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-resultado"))
                .andExpect(model().attribute("outcome", "ALREADY_USED"));
    }

    @Test
    void getVerifyEmail_unknownToken_publicAccess_rendersFailed() throws Exception {
        mvc.perform(get("/verify-email").param("token", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-resultado"))
                .andExpect(model().attribute("outcome", "EXPIRED_OR_INVALID"));
    }

    @Test
    void getVerifyEmail_missingToken_rendersFailed() throws Exception {
        mvc.perform(get("/verify-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-resultado"))
                .andExpect(model().attribute("outcome", "EXPIRED_OR_INVALID"));
    }

    @Test
    void getVerifyEmail_malformedToken_rendersFailed() throws Exception {
        mvc.perform(get("/verify-email").param("token", "not-a-uuid"))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-resultado"))
                .andExpect(model().attribute("outcome", "EXPIRED_OR_INVALID"));
    }

    @Test
    void getVerifyEmail_publicAccess_doesNotRequireAuth() throws Exception {
        // Anonymous: sem auth. Deve renderizar a tela (não redirect login).
        mvc.perform(get("/verify-email").param("token", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    // ---------- Banner integration smoke ----------

    @Test
    void homePage_authenticatedUnverified_includesBanner() throws Exception {
        Account a = persistAccount("banner-on@example.com", false);

        mvc.perform(authedAs(get("/"), a.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("email-verification-banner")));
    }

    @Test
    void verificarEmailPage_doesNotIncludeBanner() throws Exception {
        Account a = persistAccount("banner-off@example.com", false);

        // Mesmo unverified, na própria página de verificação o banner não aparece.
        mvc.perform(authedAs(get("/verificar-email"), a.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("email-verification-banner"))));
    }

}
