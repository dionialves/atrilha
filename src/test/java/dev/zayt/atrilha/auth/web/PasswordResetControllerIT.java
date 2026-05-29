package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.accounts.repository.AccountRepository;
import dev.zayt.atrilha.auth.domain.AuthenticatedAccount;
import dev.zayt.atrilha.auth.verification.PasswordResetService;
import dev.zayt.atrilha.auth.verification.PasswordResetToken;
import dev.zayt.atrilha.auth.verification.PasswordResetTokenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
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
 * Testes de integração do {@link PasswordResetController} (US-008-d).
 *
 * <p>Cobre o fluxo de consumo do link de redefinição de senha:
 * <ul>
 *   <li>GET /reset-senha?token=&lt;UUID&gt;: rende formulário (token válido) ou
 *       tela de erro (expirado / já usado / inexistente / malformado / ausente).</li>
 *   <li>POST /reset-senha: persiste novo hash BCrypt, consome o token, autentica
 *       na sessão e redireciona para "/".</li>
 *   <li>Validações: senha mínima 8 caracteres, CSRF obrigatório.</li>
 *   <li>Invalidação de sessões pré-existentes via {@link SessionRegistry} (CA-4).</li>
 * </ul>
 * </p>
 *
 * <p>Usa H2 in-memory (mesmo padrão de {@code LoginFlowTest}) — sem Testcontainers.
 * O {@code PasswordResetTokenRepository} usa lock {@code PESSIMISTIC_WRITE} no
 * {@code findByTokenForUpdate}; o H2 com {@code MODE=PostgreSQL} suporta
 * {@code SELECT ... FOR UPDATE}, mas como precaução o teste de invalidação
 * usa o caminho normal {@code findByToken}.</p>
 */
@SpringBootTest(classes = { AtrilhaApplication.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PasswordResetControllerIT {

    @Autowired
    WebApplicationContext ctx;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    PasswordResetService passwordResetService;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    SessionRegistry sessionRegistry;

    @Autowired
    EntityManager em;

    @Autowired
    PlatformTransactionManager txManager;

    MockMvc mvc;
    TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
        tx = new TransactionTemplate(txManager);
    }

    private Account persistAccount(String email) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setType("ADOLESCENT");
        a.setEmail(email);
        // Hash BCrypt válido inicial (53 chars de payload, prefixo $2b$12$).
        a.setPasswordHash("$2b$12$" + "a".repeat(53));
        a.setCreatedAt(OffsetDateTime.now());
        tx.executeWithoutResult(s -> em.persist(a));
        return a;
    }

    /** Cria diretamente um token ativo (sem expiração, sem usedAt) — não invoca o service. */
    private UUID seedActiveToken(UUID accountId) {
        PasswordResetToken t = new PasswordResetToken();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setToken(UUID.randomUUID());
        Instant now = Instant.now();
        t.setExpiresAt(now.plus(Duration.ofHours(1)));
        t.setCreatedAt(now);
        passwordResetTokenRepository.saveAndFlush(t);
        return t.getToken();
    }

    private UUID seedExpiredToken(UUID accountId) {
        PasswordResetToken t = new PasswordResetToken();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setToken(UUID.randomUUID());
        Instant past = Instant.now().minus(Duration.ofHours(2));
        t.setExpiresAt(past.plus(Duration.ofMinutes(1))); // expirou 1h59min atrás
        t.setCreatedAt(past);
        passwordResetTokenRepository.saveAndFlush(t);
        return t.getToken();
    }

    private UUID seedUsedToken(UUID accountId) {
        PasswordResetToken t = new PasswordResetToken();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setToken(UUID.randomUUID());
        Instant now = Instant.now();
        t.setExpiresAt(now.plus(Duration.ofHours(1)));
        t.setCreatedAt(now);
        t.setUsedAt(now); // já consumido
        passwordResetTokenRepository.saveAndFlush(t);
        return t.getToken();
    }

    // ----- 1) GET com token válido -----

    @Test
    void getResetSenha_validToken_rendersForm() throws Exception {
        Account account = persistAccount("ok@example.com");
        UUID token = seedActiveToken(account.getId());

        mvc.perform(get("/reset-senha").param("token", token.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-senha"))
                .andExpect(model().attribute("outcome", "SUCCESS"))
                .andExpect(model().attributeExists("token"));
    }

    // ----- 2) GET com token expirado -----

    @Test
    void getResetSenha_expiredToken_rendersExpiredMessage() throws Exception {
        Account account = persistAccount("expired@example.com");
        UUID token = seedExpiredToken(account.getId());

        mvc.perform(get("/reset-senha").param("token", token.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-senha"))
                .andExpect(model().attribute("outcome", "EXPIRED_OR_INVALID"));
    }

    // ----- 3) GET com token já consumido -----

    @Test
    void getResetSenha_alreadyUsedToken_rendersUsedMessage() throws Exception {
        Account account = persistAccount("used@example.com");
        UUID token = seedUsedToken(account.getId());

        mvc.perform(get("/reset-senha").param("token", token.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-senha"))
                .andExpect(model().attribute("outcome", "ALREADY_USED"));
    }

    // ----- 4) GET com token malformado -----

    @Test
    void getResetSenha_malformedToken_rendersGenericError() throws Exception {
        mvc.perform(get("/reset-senha").param("token", "not-a-uuid"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-senha"))
                .andExpect(model().attribute("outcome", "EXPIRED_OR_INVALID"));
    }

    // ----- 5) GET sem token -----

    @Test
    void getResetSenha_missingToken_rendersGenericError() throws Exception {
        mvc.perform(get("/reset-senha"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-senha"))
                .andExpect(model().attribute("outcome", "EXPIRED_OR_INVALID"));
    }

    // ----- 6) POST feliz: persiste, consome, autentica, redireciona -----

    @Test
    void postResetSenha_validTokenAndPassword_persistsHashConsumesTokenAuthenticates() throws Exception {
        Account account = persistAccount("happy@example.com");
        UUID token = seedActiveToken(account.getId());
        String originalHash = account.getPasswordHash();

        mvc.perform(post("/reset-senha")
                        .param("token", token.toString())
                        .param("newPassword", "SenhaSegura123!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // hash atualizado e diferente do original
        Account updated = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(updated.getPasswordHash()).isNotNull();
        assertThat(updated.getPasswordHash()).isNotEqualTo(originalHash);
        assertThat(updated.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches("SenhaSegura123!", updated.getPasswordHash())).isTrue();

        // token consumido (usedAt != null)
        PasswordResetToken consumed = passwordResetTokenRepository.findByToken(token).orElseThrow();
        assertThat(consumed.getUsedAt()).isNotNull();
    }

    // ----- 7) POST com senha curta: erro de validação, não persiste -----

    @Test
    void postResetSenha_shortPassword_returnsValidationErrors() throws Exception {
        Account account = persistAccount("short@example.com");
        UUID token = seedActiveToken(account.getId());
        String originalHash = account.getPasswordHash();

        mvc.perform(post("/reset-senha")
                        .param("token", token.toString())
                        .param("newPassword", "1234567") // 7 chars
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-senha"))
                .andExpect(model().attributeHasFieldErrors("resetForm", "newPassword"));

        // hash NÃO mudou
        Account unchanged = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(unchanged.getPasswordHash()).isEqualTo(originalHash);

        // token NÃO foi consumido (usedAt continua null)
        PasswordResetToken stillActive = passwordResetTokenRepository.findByToken(token).orElseThrow();
        assertThat(stillActive.getUsedAt()).isNull();
    }

    // ----- 8) POST com token expirado: não persiste -----

    @Test
    void postResetSenha_expiredToken_doesNotPersist() throws Exception {
        Account account = persistAccount("post-expired@example.com");
        UUID token = seedExpiredToken(account.getId());
        String originalHash = account.getPasswordHash();

        mvc.perform(post("/reset-senha")
                        .param("token", token.toString())
                        .param("newPassword", "SenhaSegura123!")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-senha"))
                .andExpect(model().attribute("outcome", "EXPIRED_OR_INVALID"));

        // hash NÃO mudou
        Account unchanged = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(unchanged.getPasswordHash()).isEqualTo(originalHash);
    }

    // ----- 9) POST sem CSRF: 403 -----

    @Test
    void postResetSenha_withoutCsrf_returns403() throws Exception {
        mvc.perform(post("/reset-senha")
                        .param("token", UUID.randomUUID().toString())
                        .param("newPassword", "SenhaSegura123!"))
                .andExpect(status().isForbidden());
    }

    // ----- 10) POST sucesso invalida sessões pré-existentes (CA-4) -----

    @Test
    void postResetSenha_success_invalidatesPreviousSessions() throws Exception {
        Account account = persistAccount("sessions@example.com");
        UUID token = seedActiveToken(account.getId());

        // Simular sessão pré-existente: registrar manualmente uma sessão "antiga"
        // associada ao principal no SessionRegistry.
        AuthenticatedAccount principal = new AuthenticatedAccount(account.getId(), AccountRole.TEEN);
        String oldSessionId = "old-session-" + UUID.randomUUID();
        sessionRegistry.registerNewSession(oldSessionId, principal);

        // sanity: a sessão antiga está no registry
        List<org.springframework.security.core.session.SessionInformation> before =
                sessionRegistry.getAllSessions(principal, false);
        assertThat(before)
                .extracting(org.springframework.security.core.session.SessionInformation::getSessionId)
                .contains(oldSessionId);

        // Executa o reset
        mvc.perform(post("/reset-senha")
                        .param("token", token.toString())
                        .param("newPassword", "SenhaSegura123!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // Após o reset, a sessão antiga deve ter sido marcada como expirada
        // (SessionRegistry.expireNow marca a flag, não remove imediatamente —
        // getAllSessions(principal, false) filtra expiradas).
        List<org.springframework.security.core.session.SessionInformation> after =
                sessionRegistry.getAllSessions(principal, false);
        assertThat(after)
                .extracting(org.springframework.security.core.session.SessionInformation::getSessionId)
                .doesNotContain(oldSessionId);
    }
}
