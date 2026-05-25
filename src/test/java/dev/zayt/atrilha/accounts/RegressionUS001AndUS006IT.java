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
    RecordingEmailSender mailer;

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
    // /health continua publico e responde 200.
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
        // confirma que a rota /cadastro/adolescente continua respondendo.
    }

    @Test
    void getCadastroAdolescenteEscolherMetodoUS002ContinuaRenderizando() throws Exception {
        // O sub-path /escolher-metodo nao deve conflitar com o GET raiz
        // /cadastro/adolescente (US-001).
        mvc.perform(get("/cadastro/adolescente/escolher-metodo"))
                .andExpect(status().isOk());
    }

    // ============================================================
    // CSRF continua bloqueando POST sem token. Os POSTs do app
    // (form email/senha) ainda devem exigir token.
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
}
