package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * REF-003: contratos observaveis apos a remocao da integracao Google OAuth.
 *
 * <p>Cobre cenarios que, se regredirem, indicam que alguem religou o flow
 * OAuth sem ajustar o restante (controllers, templates, properties):</p>
 *
 * <ul>
 *   <li>Endpoints OAuth do Spring Security ({@code /oauth2/authorization/google}
 *       e {@code /login/oauth2/code/**}) NAO devem mais existir como rotas
 *       OAuth — o filterChain perdeu o {@code .oauth2Login(...)}. A request
 *       resulta em 404 do dispatcher (sem handler) ou redireciona para login,
 *       mas NUNCA inicia um flow OAuth (302 -> accounts.google.com) e
 *       NUNCA retorna 200 num template OAuth dedicado.</li>
 *   <li>Rota {@code /cadastro/adolescente/complementar} retorna 404 — o
 *       controller {@code AdolescentGoogleSignupController} foi deletado e
 *       o template {@code adolescente_complementar.html} tambem.</li>
 *   <li>Tela {@code /cadastro/adolescente/escolher-metodo} renderiza com o
 *       botao Google estruturalmente {@code disabled} + {@code aria-disabled="true"}
 *       — usuario nao consegue mais iniciar o flow descontinuado pela UI.</li>
 * </ul>
 *
 * <p>Asserts sobre HTML usam Jsoup no DOM (nunca regex em template cru),
 * conforme convencao do projeto.</p>
 */
@SpringBootTest(classes = { AtrilhaApplication.class, OAuthRoutesRemovedIT.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OAuthRoutesRemovedIT {

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

    // ============================================================
    // /oauth2/authorization/google — flow OAuth de inicio NAO existe mais
    // ============================================================

    /**
     * Pre-REF-003 esta rota disparava um 302 para
     * {@code https://accounts.google.com/o/oauth2/v2/auth?...}. Apos a
     * remocao do {@code .oauth2Login(...)} no SecurityConfig, o
     * OAuth2AuthorizationRequestRedirectFilter nao esta mais no chain;
     * o dispatcher nao encontra handler e retorna 404 (ou outro nao-200).
     * O que NAO pode acontecer: 302 para accounts.google.com.
     */
    @Test
    void oauth2AuthorizationGoogleNaoIniciaMaisFlowGoogle() throws Exception {
        var result = mvc.perform(get("/oauth2/authorization/google")).andReturn();

        int status = result.getResponse().getStatus();
        String location = result.getResponse().getHeader("Location");

        assertThat(status)
                .as("/oauth2/authorization/google nao deve retornar 200 (template OAuth fantasma)")
                .isNotEqualTo(200);

        if (location != null) {
            assertThat(location)
                    .as("redirect (se houver) NUNCA pode apontar para accounts.google.com — flow OAuth descontinuado")
                    .doesNotContain("accounts.google.com")
                    .doesNotContain("oauth2/v2/auth");
        }
    }

    /**
     * Pre-REF-003 esta rota era o callback OAuth (Google -> app). Apos a
     * remocao, nao ha mais OAuth2LoginAuthenticationFilter no chain e a
     * rota nao tem handler. Deve responder com nao-200 (404 do dispatcher
     * ou redirect para /login pelo SecurityConfig).
     */
    @Test
    void loginOAuth2CodeGoogleNaoEhMaisCallbackValido() throws Exception {
        var result = mvc.perform(get("/login/oauth2/code/google")
                        .param("code", "fake-code")
                        .param("state", "fake-state"))
                .andReturn();

        int status = result.getResponse().getStatus();

        assertThat(status)
                .as("/login/oauth2/code/google nao deve responder 200 — callback OAuth descontinuado")
                .isNotEqualTo(200);
    }

    // ============================================================
    // /cadastro/adolescente/complementar — controller deletado
    // ============================================================

    @Test
    void cadastroAdolescenteComplementarRetorna404() throws Exception {
        var result = mvc.perform(get("/cadastro/adolescente/complementar")).andReturn();

        assertThat(result.getResponse().getStatus())
                .as("AdolescentGoogleSignupController foi removido; complementar deve dar 404")
                .isEqualTo(404);
    }

    // ============================================================
    // /cadastro/adolescente/escolher-metodo — botao Google disabled
    // (contrato estrutural; NAO valida copy/cor/layout)
    // ============================================================

    /**
     * O cliente decidiu manter o botao Google visualmente presente, mas
     * inerte. Se alguem religar a tag {@code <a th:href>} (ou remover o
     * {@code disabled}), o usuario clica e nada acontece OU pior — bate
     * em rota que nao existe e ve 404. Este teste protege o contrato:
     * o botao continua na DOM mas com {@code disabled} +
     * {@code aria-disabled="true"} (a11y) + sem {@code href}.
     */
    @Test
    void escolherMetodoRenderizaBotaoGoogleEstruturalmenteDisabled() throws Exception {
        String html = mvc.perform(get("/cadastro/adolescente/escolher-metodo"))
                .andReturn().getResponse().getContentAsString();

        Document doc = Jsoup.parse(html);

        // Selector estrutural pelo data-test (contrato com QA, nao copy).
        Element googleButton = doc.selectFirst("[data-test=cta-google-disabled]");
        assertThat(googleButton)
                .as("botao Google disabled deve continuar presente no DOM (decisao do cliente)")
                .isNotNull();

        // Estrutural: e um <button>, nao um <a th:href> (nao leva a lugar nenhum).
        assertThat(googleButton.tagName())
                .as("CTA Google deve ser <button>, nao <a> — nao pode ter href que dispare flow")
                .isEqualTo("button");

        // Estrutural: disabled (browser ignora cliques) + aria-disabled (a11y / SR).
        assertThat(googleButton.hasAttr("disabled"))
                .as("button precisa do atributo disabled — sem ele, click events disparam")
                .isTrue();
        assertThat(googleButton.attr("aria-disabled"))
                .as("aria-disabled=true e contrato de acessibilidade para leitores de tela")
                .isEqualTo("true");

        // Estrutural: nao tem href (seria invalido em <button> mas garante zero leak).
        assertThat(googleButton.hasAttr("href"))
                .as("button nao pode ter href — flow OAuth foi removido")
                .isFalse();
    }

    /**
     * Mesmo contrato no /login — onde o botao Google tambem aparece (login
     * social descontinuado). Garante que ambas as telas sao consistentes.
     */
    @Test
    void loginPageRenderizaBotaoGoogleEstruturalmenteDisabled() throws Exception {
        String html = mvc.perform(get("/login"))
                .andReturn().getResponse().getContentAsString();

        Document doc = Jsoup.parse(html);

        Element googleButton = doc.selectFirst("[data-test=cta-google-disabled]");
        assertThat(googleButton)
                .as("login.html deve manter o botao Google disabled (consistencia com escolher-metodo)")
                .isNotNull();

        assertThat(googleButton.tagName()).isEqualTo("button");
        assertThat(googleButton.hasAttr("disabled")).isTrue();
        assertThat(googleButton.attr("aria-disabled")).isEqualTo("true");
        assertThat(googleButton.hasAttr("href")).isFalse();
    }

    // ============================================================
    // Form de login (US-007) continua intacto — regressao
    // ============================================================

    /**
     * Regressao: o REF-003 NAO pode ter quebrado o form de login. Se o
     * template auth/login.html nao renderiza mais o form de e-mail/senha,
     * o usuario perde o unico caminho de autenticacao restante.
     */
    @Test
    void loginPageContinuaRenderizandoFormEmailSenhaComCsrf() throws Exception {
        String html = mvc.perform(get("/login"))
                .andReturn().getResponse().getContentAsString();

        Document doc = Jsoup.parse(html);

        Element form = doc.selectFirst("form[action=\"/login\"]");
        assertThat(form)
                .as("form de login para /login deve continuar existindo (US-007)")
                .isNotNull();
        assertThat(form.attr("method").toLowerCase())
                .as("form deve usar POST")
                .isEqualTo("post");

        assertThat(form.selectFirst("input[name=username]"))
                .as("input name=username obrigatorio").isNotNull();
        assertThat(form.selectFirst("input[name=password]"))
                .as("input name=password obrigatorio").isNotNull();
        assertThat(form.selectFirst("input[type=hidden][name=_csrf]"))
                .as("CSRF token deve estar presente no form de login")
                .isNotNull();
    }
}
