package dev.zayt.atrilha.accounts;

import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;

/**
 * Controller MVC do cadastro de responsável (US-003).
 *
 * <p>Roda o contexto completo + Postgres testcontainer porque o controller
 * depende do {@link dev.zayt.atrilha.accounts.service.RegisterGuardianService}
 * (que escreve no banco) e da validação Jakarta + CSRF + SecurityFilterChain.</p>
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
class GuardianRegistrationControllerIT {

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
    Clock clock;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    // ---------- GET /cadastro/responsavel ----------

    @Test
    void getRegistrationFormReturnsView() throws Exception {
        mvc.perform(get("/cadastro/responsavel"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/responsavel"))
                .andExpect(model().attributeExists("form"));
    }

    // ---------- POST /cadastro/responsavel — CSRF ----------

    @Test
    void postWithoutCsrfReturns403() throws Exception {
        mvc.perform(post("/cadastro/responsavel")
                        .param("email", "ok@example.com")
                        .param("password", "supersecret1")
                        .param("fullName", "Carlos")
                        .param("birthDate", "1990-05-01"))
                .andExpect(status().isForbidden());
    }

    // ---------- POST happy path ----------

    @Test
    void postValidDataRedirectsToVincular() throws Exception {
        mvc.perform(post("/cadastro/responsavel")
                        .with(csrf())
                        .param("email", "happy@example.com")
                        .param("password", "supersecret1")
                        .param("fullName", "Carlos Silva")
                        .param("birthDate", "1990-05-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vincular"));
    }

    @Test
    void postValidDataAuthenticatesSession() throws Exception {
        var session = mvc.perform(post("/cadastro/responsavel")
                        .with(csrf())
                        .param("email", "auth@example.com")
                        .param("password", "supersecret1")
                        .param("fullName", "Carlos Auth")
                        .param("birthDate", "1990-05-01"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getRequest()
                .getSession();

        // Same session followed to /vincular; expects 200, not redirect to login.
        mvc.perform(get("/vincular").session((MockHttpSession) session))
                .andExpect(status().isOk());
    }

    // ---------- POST validation errors ----------

    @Test
    void postInvalidEmailRendersFormWithFieldErrorAndKeepsOtherValues() throws Exception {
        mvc.perform(post("/cadastro/responsavel")
                        .with(csrf())
                        .param("email", "not-an-email")
                        .param("password", "supersecret1")
                        .param("fullName", "Carlos")
                        .param("birthDate", "1990-05-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/responsavel"))
                .andExpect(model().attributeHasFieldErrors("form", "email"))
                // Outros campos devem permanecer no model para repopular o form
                .andExpect(content().string(containsString("Carlos")));
    }

    @Test
    void postPasswordTooShortRendersFieldError() throws Exception {
        mvc.perform(post("/cadastro/responsavel")
                        .with(csrf())
                        .param("email", "shortpwd@example.com")
                        .param("password", "abc12")
                        .param("fullName", "shrtp")
                        .param("birthDate", "1990-05-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/responsavel"))
                .andExpect(model().attributeHasFieldErrors("form", "password"));
    }

    // ---------- POST age block — under 18 ----------

    @Test
    void postUnderageRendersBlockTemplateWithUnder18Variant() throws Exception {
        // Idade = 15 anos hoje: nascimento há 15 anos.
        mvc.perform(post("/cadastro/responsavel")
                        .with(csrf())
                        .param("email", "tooyoung@example.com")
                        .param("password", "supersecret1")
                        .param("fullName", "tooyoung")
                        .param("birthDate", LocalDate.now(clock).minusYears(15).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/responsavel_bloqueado"))
                .andExpect(model().attribute("variant", "under-18"));
    }

    // ---------- POST email duplicate ----------

    @Test
    void postDuplicateEmailRendersFieldErrorInline() throws Exception {
        // Primeiro cadastro — sucesso.
        mvc.perform(post("/cadastro/responsavel")
                        .with(csrf())
                        .param("email", "dupreg@example.com")
                        .param("password", "supersecret1")
                        .param("fullName", "Carlos Primeiro")
                        .param("birthDate", "1990-05-01"))
                .andExpect(status().is3xxRedirection());

        // Segundo cadastro com mesmo e-mail (case diferente) — deve cair em conflict.
        mvc.perform(post("/cadastro/responsavel")
                        .with(csrf())
                        .param("email", "DUPREG@example.COM")
                        .param("password", "anothersecret1")
                        .param("fullName", "Carlos Segundo")
                        .param("birthDate", "1985-04-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/responsavel"))
                .andExpect(model().attributeHasFieldErrors("form", "email"));
    }

    // ---------- US-004: GET /cadastro/responsavel/escolher-metodo ----------

    @Test
    void shouldRenderEscolherMetodoPage() throws Exception {
        mvc.perform(get("/cadastro/responsavel/escolher-metodo"))
                .andExpect(status().isOk());
    }

}
