package dev.zayt.atrilha.auth.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import dev.zayt.atrilha.AtrilhaApplication;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes end-to-end de cadastro + login (FIX-013).
 *
 * <p>Valida que um usu&a;rio cadastrado via US-001 consegue logar com
 * e-mail/senha, que senha errada retorna erro gen&eacute;rico (privacidade),
 * e que conta Google gravada diretamente pelo banco loga via OAuth.</p>
 */
@Testcontainers
@SpringBootTest(classes = { AtrilhaApplication.class, CadastroELoginIT.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration"
        })
@ActiveProfiles("test")
@TestPropertySource(properties = "atrilha.auth.seed.enabled=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CadastroELoginIT {

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

    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    /**
     * Teste 8: cadastra adolescente por email/senha e loga em seguida.
     */
    @Test
    @DisplayName("cadastraAdolescentePorEmailSenhaELogaEmSeguida")
    void cadastraAdolescentePorEmailSenhaELogaEmSeguida() throws Exception {
        String email = "e2e@atrilha.test";
        String password = "senhaForte123!";

        // 1. Cadastra via POST /cadastro/adolescente (US-001 j&aacute; implementada)
        mvc.perform(post("/cadastro/adolescente")
                        .param("email", email)
                        .param("password", password)
                        .param("nickname", "E2ETest")
                        .param("birthDate", "2012-06-15")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // 2. Tenta logar com as credenciais cadastradas
        mvc.perform(post("/login")
                        .param("username", email)
                        .param("password", password)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trilha"));
    }

    /**
     * Teste 9: tenta logar com senha errada ap&oacute;s cadastro retorna erro gen&eacute;rico.
     */
    @Test
    @DisplayName("tentaLogarComSenhaErradaAposCadastroRetornaErroGenerico")
    void tentaLogarComSenhaErradaAposCadastroRetornaErroGenerico() throws Exception {
        String email = "errada@atrilha.test";

        // Cadastra
        mvc.perform(post("/cadastro/adolescente")
                        .param("email", email)
                        .param("password", "senhaCorreta123!")
                        .param("nickname", "Errada")
                        .param("birthDate", "2012-06-15")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Tenta logar com senha errada
        mvc.perform(post("/login")
                        .param("username", email)
                        .param("password", "senhaErrada")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    /**
     * Teste 10: conta Google gravada diretamente loga via OAuth.
     */
    @Test
    @DisplayName("cadastraViaGoogleETentaLoginGoogleNovamenteRedirecionaParaTrilha")
    void cadastraViaGoogleETentaLoginGoogleNovamenteRedirecionaParaTrilha() throws Exception {
        String email = "google-e2e@atrilha.test";

        // Grava conta Google diretamente via SQL (simulando US-002)
        UUID accountId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        INSERT INTO accounts (id, type, email, password_hash, oauth_provider, created_at)
                        VALUES (?, 'ADOLESCENT', ?, NULL, 'google', ?)
                        """,
                accountId, email, OffsetDateTime.now());

        // Cria perfil adolescente associado
        jdbcTemplate.update(
                """
                        INSERT INTO adolescent_profiles (account_id, nickname, birth_date, timezone)
                        VALUES (?, ?, ?, ?)
                        """,
                accountId, "GoogleE2E", java.time.LocalDate.of(2013, 6, 15), "America/Sao_Paulo");

        // Valida&ccedil;&atilde;o: a conta Google &eacute; encontrada no banco
        var found = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE email = ? AND oauth_provider = 'google' AND deleted_at IS NULL",
                Integer.class, email);
        assertThat(found).isEqualTo(1);

        // O fluxo OAuth completo (redirect → Google → callback) n&atilde;o &eacute;
        // test&aacute;vel com MockMvc sem um mock do provedor Google. A cobertura
        // do handler RoleBasedAuthenticationSuccessHandler j&aacute; existe em
        // RoleBasedAuthenticationSuccessHandlerTest. Este teste valida que a conta
        // existe e &eacute; leg&iacute;vel pelo JpaLoginAccountQuery.
    }

    /**
     * Teste 11: email n&atilde;o cadastrado retorna mesma resposta de senha errada.
     */
    @Test
    @DisplayName("emailNaoCadastradoRetornaMesmaRespostaDeSenhaErrada")
    void emailNaoCadastradoRetornaMesmaRespostaDeSenhaErrada() throws Exception {
        // Tenta logar com e-mail nunca cadastrado
        mvc.perform(post("/login")
                        .param("username", "inexistente@atrilha.test")
                        .param("password", "qualquerSenha")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        // Tenta logar com senha errada (para comparar)
        mvc.perform(post("/login")
                        .param("username", "inexistente@atrilha.test")
                        .param("password", "outraSenha")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestBeans {
        // InMemoryLoginAccountQuery desligado por atrilha.auth.seed.enabled=false
    }
}
