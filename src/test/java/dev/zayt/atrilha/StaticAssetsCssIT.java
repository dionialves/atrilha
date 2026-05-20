package dev.zayt.atrilha;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato estrutural do CSS servido em runtime e do layout base após a
 * substituição do Tailwind Play CDN pelo build standalone do Tailwind v4
 * (chore-ux-009).
 *
 * <p>Estes testes validam o CSS gerado pelo {@code frontend-maven-plugin}
 * em {@code target/classes/static/css/app.css}. O plugin roda na fase
 * {@code process-resources}, anterior à execução dos testes, então o CSS
 * estará disponível quando o ResourceHandler do Spring Boot o servir em
 * {@code /css/app.css}.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class StaticAssetsCssIT {

    @Autowired
    WebApplicationContext ctx;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
    }

    // ============================================================
    // 1) Sanity baseline — endpoint /css/app.css responde 200 text/css
    // ============================================================

    @Test
    void cssEndpointReturns200() throws Exception {
        MvcResult result = mvc.perform(get("/css/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertThat(contentType)
                .as("Content-Type do CSS deve começar por text/css")
                .isNotNull()
                .startsWith("text/css");
    }

    // ============================================================
    // 2) Tokens de design declarados em :root (Tailwind v4 expande @theme)
    // ============================================================

    @Test
    void cssDeclaresDesignTokensAtRoot() throws Exception {
        String css = fetchCss();

        Pattern rootSelector = Pattern.compile(":root\\s*\\{");
        assertThat(rootSelector.matcher(css).find())
                .as("CSS servido deve conter um seletor :root { ... }")
                .isTrue();

        // Hex pode vir lowercase ou uppercase — o Tailwind v4 emite em lowercase
        // após minificação. O contrato é o valor coral correto, não o case.
        assertThat(css)
                .as("Token --color-primary-500 deve estar declarado com #F25C54 (case-insensitive)")
                .containsPattern("(?i)--color-primary-500:\\s*#F25C54");
        assertThat(css)
                .as("Token --space-4 deve estar declarado com 1rem")
                .containsPattern("--space-4:\\s*1rem");
        assertThat(css)
                .as("Token --space-11 deve estar declarado")
                .contains("--space-11");
        assertThat(css)
                .as("Token --radius-md deve estar declarado em .5rem ou 0.5rem")
                .containsPattern("--radius-md:\\s*0?\\.5rem");
        assertThat(css)
                .as("Token --font-sans deve incluir Inter na stack")
                .containsPattern("--font-sans:[^;]*Inter");
        assertThat(css)
                .as("Token de componente --button-primary-bg deve estar declarado")
                .contains("--button-primary-bg");
        assertThat(css)
                .as("Token de componente --input-border deve estar declarado")
                .contains("--input-border");
        assertThat(css)
                .as("Token de componente --card-bg deve estar declarado")
                .contains("--card-bg");
    }

    // ============================================================
    // 3) @theme não pode vazar cru — deve estar expandido pelo Tailwind v4
    // ============================================================

    @Test
    void cssDoesNotContainAtThemeRawSyntax() throws Exception {
        String css = fetchCss();

        assertThat(css)
                .as("CSS compilado pelo Tailwind v4 não deve conter a at-rule @theme crua")
                .doesNotContain("@theme");
    }

    // ============================================================
    // 4) base.html não pode mais ter o <script> do Play CDN
    // ============================================================

    @Test
    void baseLayoutHasNoTailwindPlayCdnScript() throws Exception {
        String html = mvc.perform(get("/comecar"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Document doc = Jsoup.parse(html);

        assertThat(doc.select("script[src*=cdn.tailwindcss]"))
                .as("Nenhum <script> apontando para cdn.tailwindcss deve sobreviver no layout")
                .isEmpty();
        assertThat(doc.select("script[src*=tailwindcss.com]"))
                .as("Nenhum <script> apontando para tailwindcss.com deve sobreviver no layout")
                .isEmpty();
    }

    // ============================================================
    // 5) Guarda-corpo: link para /css/app.css deve continuar presente
    // ============================================================

    @Test
    void baseLayoutKeepsStaticAppCssLink() throws Exception {
        String html = mvc.perform(get("/comecar"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Document doc = Jsoup.parse(html);

        assertThat(doc.select("link[rel=stylesheet][href=/css/app.css]"))
                .as("layout deve manter o <link rel=stylesheet href=/css/app.css>")
                .isNotEmpty();
    }

    // ============================================================
    // 6) Classes do design system consumidas pela US-001 sobrevivem ao build
    // ============================================================

    @Test
    void cssRetainsBaseComponentClassesFromUs001() throws Exception {
        String css = fetchCss();

        assertThat(css)
                .as("Classe .btn deve estar declarada no CSS final")
                .contains(".btn");
        assertThat(css)
                .as("Classe .btn-primary deve estar declarada no CSS final")
                .contains(".btn-primary");
        assertThat(css)
                .as("Classe .input-field deve estar declarada no CSS final")
                .contains(".input-field");
        assertThat(css)
                .as("Classe .input-group deve estar declarada no CSS final")
                .contains(".input-group");
        assertThat(css)
                .as("Classe .cadastro-form__form deve estar declarada no CSS final")
                .contains(".cadastro-form__form");
        assertThat(css)
                .as("Classe .card deve estar declarada no CSS final")
                .contains(".card");
    }

    @Test
    void cssDeclaresCardFlatVariantSelector() throws Exception {
        String css = fetchCss();

        Pattern cardFlatRule = Pattern.compile("\\.card--flat\\s*\\{");
        assertThat(cardFlatRule.matcher(css).find())
                .as("CSS servido deve conter a regra .card--flat { ... } "
                        + "(variante default da spec UX chore-ux-003 §3.3, "
                        + "usada por verificar-email, verify-email-resultado, "
                        + "cadastro/adolescente_bloqueado e cadastro/responsavel_em_breve)")
                .isTrue();
    }

    @Test
    void cssRetainsCardFlatClassNameAcrossPurge() throws Exception {
        String css = fetchCss();

        assertThat(css)
                .as("Nome da classe .card--flat deve sobreviver ao build (proteção contra "
                        + "regressão de purge ou remoção acidental do seletor literal em app.css)")
                .contains(".card--flat");
    }

    // ============================================================
    // Helpers
    // ============================================================

    private String fetchCss() throws Exception {
        return mvc.perform(get("/css/app.css"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
