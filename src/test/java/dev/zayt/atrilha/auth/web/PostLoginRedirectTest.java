package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.domain.AccountRole;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Testes de redirecionamento pós-login e placeholders (US-007 / Issue #61).
 *
 * <p>Cobre: acesso às rotas /trilha, /painel e /vincular por papel,
 * redirecionamento de anônimos, 403 para papéis inadequados e CTA na home.</p>
 */
@SpringBootTest(classes = { AtrilhaApplication.class, PostLoginRedirectTest.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PostLoginRedirectTest {

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

    // ---- /trilha ----

    @Test
    void getTrilhaAnonimoRedirecionaParaLogin() throws Exception {
        mvc.perform(get("/trilha"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void getTrilhaAutenticadoComoTeenRetorna200ERenderizaPlaceholder() throws Exception {
        AtrilhaUserDetails userDetails = createAtrilhaUserDetails("teen@atrilha.test", AccountRole.TEEN, false);
        mvc.perform(get("/trilha").sessionAttr(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        createSecurityContext(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("trilha/placeholder"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("teen"));
    }

    @Test
    void getTrilhaAutenticadoComoGuardianRetorna403() throws Exception {
        AtrilhaUserDetails userDetails = createAtrilhaUserDetails("guardian@atrilha.test", AccountRole.GUARDIAN, true);
        mvc.perform(get("/trilha").sessionAttr(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        createSecurityContext(userDetails)))
                .andExpect(status().isForbidden());
    }

    // ---- /painel ----

    @Test
    void getPainelAnonimoRedirecionaParaLogin() throws Exception {
        mvc.perform(get("/painel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void getPainelAutenticadoComoTeenRetorna403() throws Exception {
        AtrilhaUserDetails userDetails = createAtrilhaUserDetails("teen@atrilha.test", AccountRole.TEEN, false);
        mvc.perform(get("/painel").sessionAttr(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        createSecurityContext(userDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPainelAutenticadoComoGuardianVinculadoRetorna200() throws Exception {
        AtrilhaUserDetails userDetails = createAtrilhaUserDetails("guardian@atrilha.test", AccountRole.GUARDIAN, true);
        mvc.perform(get("/painel").sessionAttr(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        createSecurityContext(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("painel/placeholder"));
    }

    @Test
    void getPainelAutenticadoComoGuardianSemVinculoRetorna200() throws Exception {
        AtrilhaUserDetails userDetails = createAtrilhaUserDetails("guardian-new@atrilha.test", AccountRole.GUARDIAN, false);
        mvc.perform(get("/painel").sessionAttr(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        createSecurityContext(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("painel/placeholder"));
    }

    // ---- /vincular ----

    @Test
    void getVincularAnonimoRedirecionaParaLogin() throws Exception {
        mvc.perform(get("/vincular"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void getVincularAutenticadoComoTeenRetorna403() throws Exception {
        AtrilhaUserDetails userDetails = createAtrilhaUserDetails("teen@atrilha.test", AccountRole.TEEN, false);
        mvc.perform(get("/vincular").sessionAttr(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        createSecurityContext(userDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getVincularAutenticadoComoGuardianSemVinculoRetorna200() throws Exception {
        AtrilhaUserDetails userDetails = createAtrilhaUserDetails("guardian-new@atrilha.test", AccountRole.GUARDIAN, false);
        mvc.perform(get("/vincular").sessionAttr(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        createSecurityContext(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("vinculacao/inserir-codigo-placeholder"));
    }

    @Test
    void getVincularAutenticadoComoGuardianVinculadoRedirecionaParaPainel() throws Exception {
        AtrilhaUserDetails userDetails = createAtrilhaUserDetails("guardian@atrilha.test", AccountRole.GUARDIAN, true);
        mvc.perform(get("/vincular").sessionAttr(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        createSecurityContext(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/painel"));
    }

    // ---- home ----

    @Test
    void homePageContemCtaJaTenhoContaApontandoParaLogin() throws Exception {
        String content = mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("Já tenho conta");
        assertThat(content).contains("href=\"/login\"");
    }

}
