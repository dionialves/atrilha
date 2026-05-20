package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que o {@code SecurityConfig} esta com {@code .oauth2Login(...)}
 * habilitado e que o endpoint canonico do Spring Security
 * {@code /oauth2/authorization/google} resolve para um redirect ao
 * authorize endpoint do Google (US-002 / Issue #37).
 *
 * <p>Usa as fixtures de client em {@code application-test.properties}
 * ({@code test-client-id}/{@code test-client-secret}) — nao precisa de
 * credenciais reais para validar o handshake inicial.</p>
 */
@Testcontainers
@SpringBootTest(classes = AtrilhaApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class SecurityConfigOAuth2IT {

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

    @Test
    void endpointOauth2AuthorizationGoogleExiste() throws Exception {
        var result = mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = result.getResponse().getRedirectedUrl();
        assertThat(location)
                .as("Spring Security redireciona ao authorize endpoint do Google")
                .isNotNull()
                .startsWith("https://accounts.google.com");
        assertThat(location).contains("client_id=test-client-id");
        assertThat(location).contains("scope=");
    }
}
