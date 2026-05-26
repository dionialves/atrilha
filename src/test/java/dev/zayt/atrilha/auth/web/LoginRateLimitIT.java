package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.auth.login.LoginTestFixtures;
import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de rate-limit do login (US-007 / Issue #61).
 *
 * <p>Cobre: bloqueio após 5 tentativas falhas e expiração do bloqueio.</p>
 */
@Testcontainers
@SpringBootTest(classes = { AtrilhaApplication.class, LoginRateLimitIT.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false",
                // Encurta o bloqueio p/ validar expiração ponta-a-ponta em
                // segundos. O default de produção é 15m.
                "atrilha.auth.login.block-duration=1s"
        })
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoginRateLimitIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("atrilha")
                    .withUsername("atrilha")
                    .withPassword("atrilha");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    WebApplicationContext ctx;

    @Autowired
    LoginTestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    @BeforeEach
    void seedAccount() {
        // Container PostgreSQL é estático e persiste entre @Test;
        // delete direto via SQL torna o seed idempotente (evita duplicate key no CI).
        jdbcTemplate.execute(
            "DELETE FROM accounts WHERE LOWER(email) = LOWER('teen@atrilha.test') AND deleted_at IS NULL"
        );
        fixtures.createTeenEmailPassword("teen@atrilha.test", "test123", "teen");
    }

    @TestConfiguration
    static class TestBeans {
        // LoginTestFixtures injeta conta real via JPA no @BeforeEach seedAccount()
    }

    @Test
    void cincoTentativasFalhasSeguidasFazem6aResponderBlocked() throws Exception {
        // 5 tentativas falhas → bloqueia
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/login")
                            .param("username", "teen@atrilha.test")
                            .param("password", "ERRADA")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }

        // 6ª tentativa com senha correta → /login?blocked
        mvc.perform(post("/login")
                        .param("username", "teen@atrilha.test")
                        .param("password", "test123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?blocked"));

        // Verificar banner de rate-limited
        String content = mvc.perform(get("/login?blocked"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("data-error=\"rate-limited\"");
    }

    @Test
    void bloqueioExpiraAposBlockDurationConfigurada() throws Exception {
        // Bloquear a chave (5 falhas)
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/login")
                            .param("username", "teen@atrilha.test")
                            .param("password", "ERRADA")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }

        // Aguardar expiração do bloqueio (1.2s > 1s block-duration)
        // Documentação: única janela de tempo real necessária nesta suíte.
        // Motivo: validar comportamento de expiração ponta-a-ponta com Spring Security real.
        // Alternativa rejeitada: mockar Clock em LoginAttemptService seria intrusivo.
        Thread.sleep(1200);

        // Agora login com senha correta deve passar
        mvc.perform(post("/login")
                        .param("username", "teen@atrilha.test")
                        .param("password", "test123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trilha"));
    }
}
