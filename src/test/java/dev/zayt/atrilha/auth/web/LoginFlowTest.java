package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de fluxo completo de login (US-007 / Issue #61).
 *
 * <p>Cobre: login com credenciais corretas por papel, credenciais erradas,
 * CSRF obrigatório e persistência de sessão.
 * Testes de rate-limit estão em {@link LoginRateLimitIT}.</p>
 */
@SpringBootTest(classes = { AtrilhaApplication.class, LoginFlowTest.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoginFlowTest {

    @Autowired
    WebApplicationContext ctx;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    @TestConfiguration
    static class TestBeans {
        // InMemoryLoginAccountQuery já é carregado pelo profile "!prod"
    }

    @Test
    void postLoginComCredenciaisDeTeenRedirecionaParaTrilha() throws Exception {
        mvc.perform(post("/login")
                        .param("username", "teen@atrilha.test")
                        .param("password", "test123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trilha"));
    }

    @Test
    void postLoginComCredenciaisDeGuardianVinculadoRedirecionaParaPainel() throws Exception {
        mvc.perform(post("/login")
                        .param("username", "guardian@atrilha.test")
                        .param("password", "test123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/painel"));
    }

    @Test
    void postLoginComCredenciaisDeGuardianSemVinculoRedirecionaParaVincular() throws Exception {
        mvc.perform(post("/login")
                        .param("username", "guardian-new@atrilha.test")
                        .param("password", "test123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vincular"));
    }

    @Test
    void postLoginComSenhaErradaRedirecionaParaLoginError() throws Exception {
        mvc.perform(post("/login")
                        .param("username", "teen@atrilha.test")
                        .param("password", "ERRADA")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        // Verificar que GET /login?error exibe o banner correto
        String content = mvc.perform(get("/login?error"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("data-error=\"bad-credentials\"");
    }

    @Test
    void postLoginComEmailInexistenteRedirecionaParaLoginErrorIdenticoASenhaErrada() throws Exception {
        mvc.perform(post("/login")
                        .param("username", "ninguem@atrilha.test")
                        .param("password", "qualquer")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        // Mesmo destino que senha errada — sem oráculo
        String content = mvc.perform(get("/login?error"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("data-error=\"bad-credentials\"");
    }

    @Test
    void postLoginSemCsrfRetorna403() throws Exception {
        mvc.perform(post("/login")
                        .param("username", "teen@atrilha.test")
                        .param("password", "test123")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isForbidden());
    }

    @Test
    void postLoginSucessoCriaSessaoESustentaRequisicaoSeguinte() throws Exception {
        // Login com sucesso — capturar sessão
        MvcResult loginResult = mvc.perform(post("/login")
                        .param("username", "teen@atrilha.test")
                        .param("password", "test123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        org.springframework.mock.web.MockHttpSession session = (org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false);

        // GET /trilha com a sessão → 200
        mvc.perform(get("/trilha").session(session))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view()
                        .name("trilha/placeholder"));

        // GET /trilha SEM a sessão → 302 para login
        mvc.perform(get("/trilha"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void postLogoutComCsrfRedirecionaParaLoginLogout() throws Exception {
        // Primeiro logar
        mvc.perform(post("/login")
                        .param("username", "teen@atrilha.test")
                        .param("password", "test123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Logout com CSRF
        mvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        // GET /trilha em seguida → 302 para login (sessão invalidada)
        mvc.perform(get("/trilha"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}


