package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.auth.AccountRole;
import dev.zayt.atrilha.auth.AuthenticatedAccount;
import dev.zayt.atrilha.auth.PendingGoogleSignup;
import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * MVC do {@code AdolescentGoogleSignupController} (US-002 / Issue #37).
 *
 * <p>Cobre as 3 rotas novas:
 * <ul>
 *   <li>{@code GET /cadastro/adolescente/escolher-metodo} (Tela 2)</li>
 *   <li>{@code GET/POST /cadastro/adolescente/complementar} (Tela 3)</li>
 * </ul>
 * </p>
 *
 * <p>Reusa o {@code AdolescentRegistrationController} (US-001) intacto — as
 * rotas convivem sem conflito.</p>
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.baseline-on-migrate=false"
})
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class AdolescentGoogleSignupControllerIT {

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
    WebApplicationContext ctx;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    Clock clock;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    private static PendingGoogleSignup pending(String email) {
        return new PendingGoogleSignup(
                email,
                OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                "Julia",
                "https://lh3.googleusercontent.com/a/julia",
                Instant.parse("2026-05-20T10:00:00Z"));
    }

    private static MockHttpSession sessionWithPending(PendingGoogleSignup p) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("pendingGoogleSignup", p);
        return s;
    }

    // 15
    @Test
    void getEscolherMetodoRenderizaBotaoGoogle() throws Exception {
        mvc.perform(get("/cadastro/adolescente/escolher-metodo"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente_escolher_metodo"))
                .andExpect(content().string(containsString("/oauth2/authorization/google")))
                .andExpect(content().string(containsString("/cadastro/adolescente")));
    }

    // 16
    @Test
    void getComplementarSemPendingRedirecionaEscolherMetodo() throws Exception {
        mvc.perform(get("/cadastro/adolescente/complementar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/adolescente/escolher-metodo"));
    }

    // 17
    @Test
    void getComplementarComPendingRenderizaForm() throws Exception {
        var p = pending("julia.complementar@gmail.com");
        mvc.perform(get("/cadastro/adolescente/complementar").session(sessionWithPending(p)))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente_complementar"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attributeExists("pending"))
                .andExpect(content().string(containsString("julia.complementar@gmail.com")));
    }

    @Test
    void getComplementarPreenchimentoSugestaoApelidoTruncadoA20() throws Exception {
        var p = new PendingGoogleSignup(
                "longname@gmail.com",
                OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                "AbcdefghijklmnopqrstuvWxyz",  // 26 chars
                null,
                Instant.parse("2026-05-20T10:00:00Z"));
        MvcResult result = mvc.perform(get("/cadastro/adolescente/complementar")
                        .session(sessionWithPending(p)))
                .andExpect(status().isOk())
                .andReturn();
        CompleteGoogleSignupForm form = (CompleteGoogleSignupForm)
                result.getModelAndView().getModel().get("form");
        assertThat(form.getNickname()).hasSizeLessThanOrEqualTo(20);
        assertThat(form.getNickname()).isEqualTo("Abcdefghijklmnopqrst");
    }

    // 18
    @Test
    void postComplementarSucessoCriaContaERedirecionaConcluido() throws Exception {
        var p = pending("julia.post@gmail.com");
        MockHttpSession session = sessionWithPending(p);

        MvcResult result = mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(session)
                        .with(csrf())
                        .param("nickname", "juliap")
                        .param("birthDate", "2010-05-01")
                        .param("photoSource", "NONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/concluido"))
                .andReturn();

        // Conta criada
        Account acc = accountRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("julia.post@gmail.com")
                .orElseThrow();
        assertThat(acc.getOauthProvider()).isEqualTo("google");

        // Sessao Spring Security autenticada com role TEEN
        MockHttpSession returnedSession = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(returnedSession).isNotNull();
        SecurityContext sc = (SecurityContext)
                returnedSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(sc).isNotNull();
        assertThat(sc.getAuthentication().getPrincipal()).isInstanceOf(AuthenticatedAccount.class);
        AuthenticatedAccount principal = (AuthenticatedAccount) sc.getAuthentication().getPrincipal();
        assertThat(principal.id()).isEqualTo(acc.getId());
        assertThat(principal.role()).isEqualTo(AccountRole.TEEN);
        assertThat(sc.getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_TEEN");

        // pendingGoogleSignup removido da sessao
        assertThat(returnedSession.getAttribute("pendingGoogleSignup")).isNull();
    }

    // 19
    @Test
    void postComplementarIdadeInvalidaRenderizaBloqueioELimpaSessao() throws Exception {
        var p = pending("idade.invalida@gmail.com");
        MockHttpSession session = sessionWithPending(p);
        long before = accountRepository.count();

        MvcResult result = mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(session)
                        .with(csrf())
                        .param("nickname", "kidd")
                        .param("birthDate", LocalDate.now(clock).minusYears(10).toString())
                        .param("photoSource", "NONE"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente_bloqueado"))
                .andExpect(model().attribute("variant", "under-13"))
                .andReturn();

        assertThat(accountRepository.count()).isEqualTo(before);
        MockHttpSession returned = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(returned.getAttribute("pendingGoogleSignup"))
                .as("idade invalida limpa sessao antes do bloqueio")
                .isNull();
    }

    @Test
    void postComplementarMaiorDe17RenderizaBloqueioOver17() throws Exception {
        var p = pending("maior.idade@gmail.com");
        MockHttpSession session = sessionWithPending(p);

        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(session)
                        .with(csrf())
                        .param("nickname", "older")
                        .param("birthDate", LocalDate.now(clock).minusYears(25).toString())
                        .param("photoSource", "NONE"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente_bloqueado"))
                .andExpect(model().attribute("variant", "over-17"));
    }

    // 20
    @Test
    void postComplementarEmailConflictRedirecionaEscolherMetodoComErro() throws Exception {
        // Pre-cria conta com mesmo e-mail (via fluxo Google).
        var p1 = pending("conflito@gmail.com");
        MockHttpSession s1 = sessionWithPending(p1);
        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(s1)
                        .with(csrf())
                        .param("nickname", "primeiro")
                        .param("birthDate", "2010-05-01")
                        .param("photoSource", "NONE"))
                .andExpect(status().is3xxRedirection());

        // Nova tentativa, mesmo e-mail (case diferente, novo pending).
        var p2 = new PendingGoogleSignup(
                "CONFLITO@gmail.com",
                OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                "Other", null,
                Instant.parse("2026-05-20T10:00:00Z"));
        MockHttpSession s2 = sessionWithPending(p2);
        MvcResult result = mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(s2)
                        .with(csrf())
                        .param("nickname", "segundo")
                        .param("birthDate", "2010-05-01")
                        .param("photoSource", "NONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/adolescente/escolher-metodo?error=account_exists"))
                .andReturn();

        MockHttpSession returned = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(returned.getAttribute("pendingGoogleSignup"))
                .as("apos conflito, pending e removido da sessao")
                .isNull();
    }

    // 21
    @Test
    void getConcluidoRenderizaPlaceholder() throws Exception {
        // (duplicado em SignupEntryControllerIT mas mantido aqui porque a
        // Ordem TDD numera esse teste dentro deste controller IT.)
        mvc.perform(get("/cadastro/concluido"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/concluido"));
    }

    // Extra: cancel=1 limpa a sessao e redireciona
    @Test
    void getComplementarComCancelLimpaSessao() throws Exception {
        var p = pending("cancel@gmail.com");
        MockHttpSession session = sessionWithPending(p);
        mvc.perform(get("/cadastro/adolescente/complementar?cancel=1").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/adolescente/escolher-metodo"));
        assertThat(session.getAttribute("pendingGoogleSignup")).isNull();
    }

    // Extra: GET escolher-metodo com ?error=cancelled adiciona ao model
    @Test
    void getEscolherMetodoComErrorAdicionaAoModel() throws Exception {
        mvc.perform(get("/cadastro/adolescente/escolher-metodo?error=cancelled"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "cancelled"));
    }

    // CSRF
    @Test
    void postComplementarSemCsrfRetorna403() throws Exception {
        var p = pending("csrf@gmail.com");
        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(sessionWithPending(p))
                        .param("nickname", "abcd")
                        .param("birthDate", "2010-05-01")
                        .param("photoSource", "NONE"))
                .andExpect(status().isForbidden());
    }

    // Validation error sem ser idade — mantem na tela complementar
    @Test
    void postComplementarApelidoInvalidoRenderizaForm() throws Exception {
        var p = pending("invalid.nick@gmail.com");
        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(sessionWithPending(p))
                        .with(csrf())
                        .param("nickname", "ab")
                        .param("birthDate", "2010-05-01")
                        .param("photoSource", "NONE"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente_complementar"))
                .andExpect(model().attributeHasFieldErrors("form", "nickname"));
    }
}
