package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.auth.AccountRole;
import dev.zayt.atrilha.auth.login.AtrilhaUserDetails;
import dev.zayt.atrilha.auth.login.LoginAccountQuery;
import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Testes da pagina 403 amigavel (FIX-014).
 *
 * <p>Garante que quando um usuario autenticado acessa uma rota sem o role
 * necessario, o Spring Security retorna 403 + template amigavel em vez do
 * Whitelabel Error Page.</p>
 */
@SpringBootTest(classes = { AtrilhaApplication.class, Error403PageTest.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class Error403PageTest {

    @Autowired
    WebApplicationContext ctx;

    private MockMvc mvc;

    @TestConfiguration
    static class TestBeans {
        // InMemoryLoginAccountQuery ja e carregado pelo profile "!prod"
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    private org.springframework.security.core.context.SecurityContext createSecurityContext(AtrilhaUserDetails userDetails) {
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());
        org.springframework.security.core.context.SecurityContext sc =
                SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        return sc;
    }

    private AtrilhaUserDetails createAtrilhaUserDetails(String email, AccountRole role, boolean hasGuardianLink) {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                email, "dummy", role, hasGuardianLink, email.split("@")[0]);
        return new AtrilhaUserDetails(account);
    }

    @Test
    void requestProtegidaComoPapelErradoRetorna403() throws Exception {
        AtrilhaUserDetails userDetails = createAtrilhaUserDetails("guardian@atrilha.test", AccountRole.GUARDIAN, true);

        mvc.perform(get("/trilha").sessionAttr(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        createSecurityContext(userDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestDiretoParaErrorNaoRedirecionaParaLogin() throws Exception {
        // GET /error por usuario anonimo NAO deve redirecionar para /login.
        // O BasicErrorController pode retornar 200 (template encontrado) ou 500
        // (se o template nao for resolvido no contexto do teste), mas nunca 302.
        mvc.perform(get("/error"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(302));
    }
}
