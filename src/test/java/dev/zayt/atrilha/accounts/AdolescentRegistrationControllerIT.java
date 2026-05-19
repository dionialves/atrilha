package dev.zayt.atrilha.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;

/**
 * Controller MVC do cadastro de adolescente (US-001).
 *
 * <p>Roda o contexto completo + Postgres testcontainer porque o controller
 * depende do {@link RegisterAdolescentService} (que escreve no banco) e da
 * validação Jakarta + CSRF + SecurityFilterChain.</p>
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.baseline-on-migrate=false"
})
@ActiveProfiles("test")
@DirtiesContext
class AdolescentRegistrationControllerIT {

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

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    // ---------- GET /comecar ----------

    @Test
    void getComecarRendersBothPaths() throws Exception {
        mvc.perform(get("/comecar"))
                .andExpect(status().isOk())
                .andExpect(view().name("comecar"))
                .andExpect(content().string(containsString("Sou adolescente")))
                .andExpect(content().string(containsString("Sou responsável")));
    }

    // ---------- GET /cadastro/adolescente ----------

    @Test
    void getRegistrationFormReturnsView() throws Exception {
        mvc.perform(get("/cadastro/adolescente"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente"))
                .andExpect(model().attributeExists("form"));
    }

    // ---------- POST /cadastro/adolescente — CSRF ----------

    @Test
    void postWithoutCsrfReturns403() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .param("email", "ok@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "kira")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().isForbidden());
    }

    // ---------- POST happy path ----------

    @Test
    void postValidDataRedirectsToVerifyEmail() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "happy@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "happy")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verificar-email"));
    }

    @Test
    void postValidDataAuthenticatesSession() throws Exception {
        var session = mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "auth@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "auth1")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getRequest()
                .getSession();

        // Same session followed to /verificar-email; expects 200, not redirect to login.
        mvc.perform(get("/verificar-email").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk());
    }

    // ---------- POST validation errors ----------

    @Test
    void postInvalidEmailRendersFormWithFieldErrorAndKeepsOtherValues() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "not-an-email")
                        .param("password", "supersecret1")
                        .param("nickname", "kept")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente"))
                .andExpect(model().attributeHasFieldErrors("form", "email"))
                // Outros campos devem permanecer no model para repopular o form
                .andExpect(content().string(containsString("kept")));
    }

    @Test
    void postPasswordTooShortRendersFieldError() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "shortpwd@example.com")
                        .param("password", "abc12")
                        .param("nickname", "shrtp")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente"))
                .andExpect(model().attributeHasFieldErrors("form", "password"));
    }

    @Test
    void postBlankNicknameRendersFieldError() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "ok@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "ab")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente"))
                .andExpect(model().attributeHasFieldErrors("form", "nickname"));
    }

    // ---------- POST age block — under 13 ----------

    @Test
    void postUnderageRendersBlockTemplateWithUnder13Variant() throws Exception {
        // Idade = 10 anos hoje (2026-05-19): nascimento em 2016-05-01.
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "tooyoung@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "tooyoung")
                        .param("birthDate", LocalDate.now().minusYears(10).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente_bloqueado"))
                .andExpect(model().attribute("variant", "under-13"));
    }

    // ---------- POST age block — 18+ ----------

    @Test
    void postOverAgeRendersBlockTemplateWithOver17Variant() throws Exception {
        // Idade = 25 anos: nascimento há 25 anos.
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "tooold@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "tooold")
                        .param("birthDate", LocalDate.now().minusYears(25).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente_bloqueado"))
                .andExpect(model().attribute("variant", "over-17"));
    }

    // ---------- POST email duplicate ----------

    @Test
    void postDuplicateEmailRendersFieldErrorInline() throws Exception {
        // Primeiro cadastro — sucesso.
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "dupreg@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "first1")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().is3xxRedirection());

        // Segundo cadastro com mesmo e-mail (case diferente) — deve cair em conflict.
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "DUPREG@example.com")
                        .param("password", "anothersecret")
                        .param("nickname", "second")
                        .param("birthDate", "2009-04-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente"))
                .andExpect(model().attributeHasFieldErrors("form", "email"));
    }

    // ---------- POST with multipart photo ----------

    @Test
    void postWithValidMultipartPhotoRedirectsToVerifyEmail() throws Exception {
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "selfie.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});

        mvc.perform(multipart("/cadastro/adolescente")
                        .file(photo)
                        .with(csrf())
                        .param("email", "withphoto2@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "withphoto")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verificar-email"));
    }

    // ---------- GET /cadastro/responsavel stub ----------

    @Test
    void getCadastroResponsavelRendersComingSoonStub() throws Exception {
        mvc.perform(get("/cadastro/responsavel"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/responsavel_em_breve"));
    }

}
