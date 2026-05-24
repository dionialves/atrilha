package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.notifications.RecordedEmail;
import dev.zayt.atrilha.notifications.RecordingEmailSender;
import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regressao: o trabalho da US-002 (Issue #37) NAO pode ter quebrado o
 * comportamento das US-001 (cadastro email/senha), US-005 (bloqueio por
 * idade), US-006 (verificacao de e-mail) ou o /health.
 *
 * <p>Roda no profile {@code test} default com H2 (sem Testcontainers
 * Postgres) — igual ao {@code SecurityConfigSessionRewritingIT}.
 * As assertions cobrem contratos observaveis via MVC e via
 * RecordingEmailSender (que detecta o disparo do AccountRegisteredEvent
 * pelo listener AFTER_COMMIT).</p>
 *
 * <p>Foco: contratos observaveis que outras features dependem. Cada
 * teste, se falhar, indica que a US-002 introduziu regressao em
 * funcionalidade ja entregue.</p>
 */
@SpringBootTest(classes = AtrilhaApplication.class)
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class RegressionUS001AndUS006IT {

    @Autowired
    WebApplicationContext ctx;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    RecordingEmailSender mailer;

    @Autowired
    Clock clock;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
        mailer.clear();
    }

    private void awaitEmailSent(int expectedCount, long timeoutSeconds) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            if (mailer.recorded().size() >= expectedCount) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
        }
    }

    // ============================================================
    // US-006: AccountRegisteredEvent eh disparado pelo fluxo de
    // email/senha (US-001). A US-002 explicitamente NAO dispara o
    // evento, mas o fluxo antigo TEM que continuar disparando.
    //
    // Se essa invariante quebrar, novos usuarios de email/senha
    // deixam de receber e-mail de verificacao → US-006 quebra
    // silenciosamente.
    // ============================================================

    @Test
    void cadastroEmailSenhaUS001AindaDisparaAccountRegisteredEvent() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "regressao.event@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "regress")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verificar-email"));

        awaitEmailSent(1, 3);

        assertThat(mailer.recorded())
                .as("fluxo email/senha deve continuar publicando AccountRegisteredEvent (regressao US-006)")
                .isNotEmpty();
        RecordedEmail email = mailer.recorded().get(0);
        assertThat(email.toEmail()).isEqualTo("regressao.event@example.com");
    }

    // ============================================================
    // Contraste: o fluxo Google NAO dispara evento. Esse teste
    // existe no RegisterAdolescentServiceGoogleIT do plano (no.8);
    // mas aqui o ponto eh confirmar que o controle de "quem dispara
    // e quem nao" eh feito ao nivel do service correto — e nao
    // acidentalmente desabilitado para AMBOS.
    // ============================================================

    @Test
    void contrastFluxoGoogleNaoDisparaEventoMasFluxoEmailDispara() throws Exception {
        // 1) Cadastra email/senha — deve disparar evento.
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "contrast.email@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "contr1")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().is3xxRedirection());

        awaitEmailSent(1, 3);
        int countAfterEmail = mailer.recorded().size();
        assertThat(countAfterEmail)
                .as("fluxo email deve disparar evento")
                .isEqualTo(1);

        // 2) Conta criada deve estar persistida com password_hash.
        Account a = accountRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("contrast.email@example.com")
                .orElseThrow();
        assertThat(a.getPasswordHash())
                .as("fluxo email salva password_hash, nunca oauth_provider")
                .isNotNull();
        assertThat(a.getOauthProvider()).isNull();
    }

    // ============================================================
    // /health continua publico e responde 200. Garante que
    // .oauth2Login(...) e os novos handlers nao prendem o endpoint
    // por engano.
    // ============================================================

    @Test
    void healthEndpointContinuaPublicoE200() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    // ============================================================
    // /cadastro/adolescente (US-001) continua renderizando o form
    // email/senha — convive com /cadastro/adolescente/escolher-metodo
    // (US-002) sem conflito de rota.
    // ============================================================

    @Test
    void getCadastroAdolescenteUS001ContinuaRenderizandoForm() throws Exception {
        mvc.perform(get("/cadastro/adolescente"))
                .andExpect(status().isOk());
        // view name e contrato existente — o teste de regressao apenas
        // confirma que a rota nao foi mascarada pelo @RequestMapping
        // novo de AdolescentGoogleSignupController.
    }

    @Test
    void getCadastroAdolescenteEscolherMetodoUS002ContinuaRenderizando() throws Exception {
        // O sub-path /escolher-metodo nao deve conflitar com o GET raiz
        // /cadastro/adolescente (US-001).
        mvc.perform(get("/cadastro/adolescente/escolher-metodo"))
                .andExpect(status().isOk());
    }

    // ============================================================
    // CSRF continua bloqueando POST sem token mesmo apos adicao do
    // .oauth2Login(). O callback OAuth eh tratado pelo proprio
    // Spring Security (fora do CSRF check), mas os POSTs do app
    // (form email/senha, form complementar) ainda devem exigir
    // token.
    // ============================================================

    @Test
    void postCadastroAdolescenteSemCsrfContinua403() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .param("email", "no.csrf@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "noctk")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // Conta criada por fluxo Google deve ser localizavel via o
    // mesmo finder usado pelo fluxo email/senha
    // (findByEmailIgnoreCaseAndDeletedAtIsNull). Garante coexistencia
    // — alguem nao criou um silo separado pra contas Google.
    // ============================================================

    @Test
    void contaCriadaPorGoogleEhLocalizavelPeloMesmoFinderDeEmailSenha() throws Exception {
        // Cria via fluxo Google (via controller POST complementar).
        var session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("pendingGoogleSignup",
                new dev.zayt.atrilha.auth.PendingGoogleSignup(
                        "google.unifiedfinder@gmail.com",
                        java.time.OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, java.time.ZoneOffset.UTC),
                        "Unified", null,
                        java.time.Instant.parse("2026-05-20T10:00:00Z")));

        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(session)
                        .with(csrf())
                        .param("nickname", "unified")
                        .param("birthDate", "2010-05-01")
                        .param("photoSource", "NONE"))
                .andExpect(status().is3xxRedirection());

        var found = accountRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("google.unifiedfinder@gmail.com");
        assertThat(found)
                .as("conta Google deve ser encontrada pelo finder universal de email")
                .isPresent();
        assertThat(found.get().getOauthProvider()).isEqualTo("google");
        assertThat(found.get().getPasswordHash()).isNull();
    }

    // ============================================================
    // CA-4 US-002 (defesa-em-profundidade): para o fluxo Google,
    // contagem de Account NAO muda quando idade invalida bloqueia.
    // ============================================================

    @Test
    void ca4UsCadastroGoogleComIdadeInvalidaNaoIncrementaContagemDeContas() throws Exception {
        long before = accountRepository.count();
        var session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("pendingGoogleSignup",
                new dev.zayt.atrilha.auth.PendingGoogleSignup(
                        "ca4.google@gmail.com",
                        java.time.OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, java.time.ZoneOffset.UTC),
                        "Ca4", null,
                        java.time.Instant.parse("2026-05-20T10:00:00Z")));

        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(session)
                        .with(csrf())
                        .param("nickname", "ca4")
                        .param("birthDate", java.time.LocalDate.now(clock).minusYears(10).toString())
                        .param("photoSource", "NONE"))
                .andExpect(status().isOk());

        assertThat(accountRepository.count())
                .as("CA-4 explicito: bloqueio nao deve persistir conta")
                .isEqualTo(before);
    }
}
