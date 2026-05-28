package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.testsupport.AbstractSpringPostgresIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import dev.zayt.atrilha.AtrilhaApplication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes end-to-end de cadastro + login (FIX-013).
 *
 * <p>Valida que um usu&a;rio cadastrado via US-001 consegue logar com
 * e-mail/senha e que senha errada retorna erro gen&eacute;rico (privacidade).</p>
 */
@SpringBootTest(classes = { AtrilhaApplication.class, CadastroELoginIT.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration"
        })
@ActiveProfiles("test")
@TestPropertySource(properties = "atrilha.auth.seed.enabled=false")
class CadastroELoginIT extends AbstractSpringPostgresIT {

    @Autowired
    WebApplicationContext ctx;

    private MockMvc mvc;

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
     * Teste 10: email n&atilde;o cadastrado retorna mesma resposta de senha errada.
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
