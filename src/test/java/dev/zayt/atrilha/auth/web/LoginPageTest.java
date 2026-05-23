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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Testes da página de login (US-007 / Issue #61).
 *
 * <p>Cobre: acesso anônimo, banners de erro/informativo por querystring,
 * e redirecionamento quando já autenticado (TEEN → /trilha, GUARDIAN
 * vinculado → /painel, GUARDIAN sem vínculo → /vincular).</p>
 */
@SpringBootTest(classes = { AtrilhaApplication.class, LoginPageTest.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoginPageTest {

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

    @Test
    void getLoginAnonimoRetorna200ComFormulario() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("name=\"username\""))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("name=\"password\""))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("type=\"submit\""))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("href=\"/oauth2/authorization/google\""))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("name=\"_csrf\""));
    }

    @Test
    void getLoginAnonimoNaoExibeNenhumBannerDeErro() throws Exception {
        String content = mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).doesNotContain("data-error=\"bad-credentials\"");
        assertThat(content).doesNotContain("data-error=\"rate-limited\"");
        assertThat(content).doesNotContain("data-state=\"logged-out\"");
    }

    @Test
    void getLoginComQueryErrorExibeBannerBadCredentials() throws Exception {
        mvc.perform(get("/login?error"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("data-error=\"bad-credentials\""))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("data-error=\"rate-limited\""));
    }

    @Test
    void getLoginComQueryBlockedExibeBannerRateLimited() throws Exception {
        String content = mvc.perform(get("/login?blocked"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("data-error=\"rate-limited\"");
        // Inputs username e password ficam disabled
        assertThat(content).containsPattern("name=\"username\"[^>]*disabled");
        // Google button NUNCA disabled
        assertThat(content).contains("href=\"/oauth2/authorization/google\"");
    }

    @Test
    void getLoginComQueryLogoutExibeBannerInformativo() throws Exception {
        mvc.perform(get("/login?logout"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("data-state=\"logged-out\""))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("data-error=\"bad-credentials\""))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("data-error=\"rate-limited\""));
    }

    @Test
    void getLoginAutenticadoComoTeenRedirecionaParaTrilha() throws Exception {
        AtrilhaUserDetails userDetails = createAtrilhaUserDetails("teen@atrilha.test", AccountRole.TEEN, false);
        mvc.perform(get("/login").sessionAttr(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        createSecurityContext(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trilha"));
    }

    @Test
    void getLoginAutenticadoComoGuardianVinculadoRedirecionaParaPainel() throws Exception {
        AtrilhaUserDetails userDetails = createAtrilhaUserDetails("guardian@atrilha.test", AccountRole.GUARDIAN, true);
        mvc.perform(get("/login").sessionAttr(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        createSecurityContext(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/painel"));
    }

    @Test
    void getLoginAutenticadoComoGuardianSemVinculoRedirecionaParaVincular() throws Exception {
        AtrilhaUserDetails userDetails = createAtrilhaUserDetails("guardian-new@atrilha.test", AccountRole.GUARDIAN, false);
        mvc.perform(get("/login").sessionAttr(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        createSecurityContext(userDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vincular"));
    }
}
