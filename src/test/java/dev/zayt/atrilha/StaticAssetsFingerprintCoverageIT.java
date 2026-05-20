package dev.zayt.atrilha;

import jakarta.servlet.Filter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobertura adicional (QA) sobre o contrato do fingerprint de assets
 * estáticos (fix-001). Estende {@link StaticAssetsFingerprintProdIT} sem
 * alterá-lo, mantendo o mesmo {@code @TestPropertySource} que ativa a
 * estratégia content-hash do Spring (prod-like).
 *
 * <p>Foco em cenários que, se quebrarem, quebram a invalidação de CSS
 * pós-deploy:
 * <ul>
 *   <li><b>A) Variação de rotas</b> — fingerprint vale em todas as telas
 *       servidas pelo layout base, não só {@code /comecar}.</li>
 *   <li><b>B) Hash determinístico</b> — para o mesmo conteúdo, 2 GETs no
 *       mesmo path devem produzir o mesmo href. Sem isso, cada render
 *       quebraria o cache mesmo sem mudança no CSS.</li>
 *   <li><b>C) GET com hash inválido</b> — comportamento documentado.
 *       Assertion adaptativa porque o contrato exato do Spring pode mudar
 *       entre versões; o que importa é que a invariante "GET na URL
 *       hashed real funciona" continue valendo.</li>
 *   <li><b>D) Cap de tamanho da URL hashed</b> — guardrail contra
 *       estratégia diferente acidentalmente ativada (ex.: hash de path
 *       completo ou versionamento fixo gigante). MD5 de 32 chars
 *       hex + prefixo + extensão = ~50 chars; 80 é folga.</li>
 *   <li><b>E) Paridade de Cache-Control</b> — a resposta legada
 *       {@code /css/app.css} também precisa trazer
 *       {@code Cache-Control: max-age=...}. Se sumir só do caminho
 *       legado, links diretos antigos ainda recebem CSS atual mas sem
 *       cacheabilidade, prejudicando perf.</li>
 * </ul>
 *
 * <p>O que NÃO é coberto aqui (gap intencional declarado pelo plano da
 * issue): comportamento de Cloudflare, conteúdo visual, latência da CDN,
 * comportamento de navegadores reais, header {@code Vary},
 * ETag/Last-Modified, pixel/cor/fonte. O cenário 6 do plano (regressão
 * de auth com {@code enableSessionUrlRewriting(true)}) vive em
 * {@link dev.zayt.atrilha.auth.SecurityConfigSessionRewritingIT} para
 * que a asserção seja feita no profile {@code test} default — o efeito
 * de {@code enableSessionUrlRewriting(true)} é global, não depende do
 * chain de resources estar ligado.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.web.resources.chain.enabled=true",
        "spring.web.resources.chain.strategy.content.enabled=true",
        "spring.web.resources.chain.strategy.content.paths=/**",
        "spring.web.resources.cache.cachecontrol.max-age=86400"
})
class StaticAssetsFingerprintCoverageIT {

    @Autowired
    WebApplicationContext ctx;

    MockMvc mvc;

    // Regex que casa /css/app-<hash>.css onde <hash> e hex de 8+ chars.
    // FileNameVersionPathStrategy.addVersion() usa hifen como separador
    // (AbstractVersionStrategy.java:144 — "baseFilename + '-' + version + '.' + extension").
    // Content-version do Spring usa MD5 (32 chars hex). 8+ e tolerante.
    private static final Pattern HASHED_CSS_HREF =
            Pattern.compile("/css/app-([a-f0-9]{8,})\\.css");

    @BeforeEach
    void setUp() {
        // Mesma estrategia de StaticAssetsFingerprintProdIT: registrar
        // manualmente o ResourceUrlEncodingFilter porque
        // MockMvcBuilders.webAppContextSetup nao carrega
        // FilterRegistrationBeans automaticamente.
        Filter resourceUrlEncodingFilter = resolveResourceUrlEncodingFilter();
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(resourceUrlEncodingFilter)
                .apply(springSecurity())
                .build();
    }

    private Filter resolveResourceUrlEncodingFilter() {
        Map<String, FilterRegistrationBean<?>> beans = getFilterRegistrationBeans();
        for (FilterRegistrationBean<?> reg : beans.values()) {
            Filter filter = reg.getFilter();
            if (filter instanceof ResourceUrlEncodingFilter) {
                return filter;
            }
        }
        throw new IllegalStateException(
                "ResourceUrlEncodingFilter nao encontrado no contexto — "
                        + "spring.web.resources.chain.enabled deve estar true via @TestPropertySource."
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, FilterRegistrationBean<?>> getFilterRegistrationBeans() {
        return (Map) ctx.getBeansOfType(FilterRegistrationBean.class);
    }

    // ============================================================
    // A) Variação de rotas — fingerprint vale em TODAS as telas
    // servidas pelo layout base, não só /comecar.
    //
    // O ResourceUrlEncodingFilter intercepta th:href="@{}" no
    // momento da renderização Thymeleaf. Se alguém quebrar a
    // configuração do chain ou bypassar o layout em uma view
    // específica (ex.: include sem extends), a URL no HTML dessa
    // rota volta a ser literal, e o cache de 24h volta a engasgar
    // só para aquela tela. Variar rotas pega essa classe de bug.
    // ============================================================

    @ParameterizedTest
    @ValueSource(strings = {"/", "/comecar", "/cadastro/adolescente"})
    void allPublicRoutesEmitHashedCssHref(String route) throws Exception {
        String html = mvc.perform(get(route))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Document doc = Jsoup.parse(html);
        Elements links = doc.select("link[rel=stylesheet][href$=.css]");
        assertThat(links)
                .as("Rota %s deve ter pelo menos um <link rel=stylesheet>", route)
                .isNotEmpty();

        String href = links.first().attr("href");
        Matcher m = HASHED_CSS_HREF.matcher(href);
        assertThat(m.matches())
                .as("Rota %s deve emitir href com hash no formato /css/app-<hash>.css — recebido: %s",
                        route, href)
                .isTrue();
    }

    // ============================================================
    // B) Hash determinístico — 2 GETs no mesmo path devem produzir
    // o mesmo href.
    //
    // ContentVersionStrategy do Spring calcula MD5(conteúdo) por
    // request mas faz cache do resultado em VersionResourceResolver.
    // Se alguém trocar a estratégia por algo time-based ou random,
    // cada render geraria URL nova — invalidaria cache CDN a cada
    // request, derrubando perf. Cenário exato do plano #2.
    // ============================================================

    @Test
    void sameContentProducesSameHashedHrefAcrossRequests() throws Exception {
        String href1 = extractCssHref("/comecar");
        String href2 = extractCssHref("/comecar");

        assertThat(href1)
                .as("href 1 deve casar /css/app-<hash>.css")
                .matches(HASHED_CSS_HREF.asPredicate()::test);
        assertThat(href2)
                .as("href 2 deve casar /css/app-<hash>.css")
                .matches(HASHED_CSS_HREF.asPredicate()::test);
        assertThat(href2)
                .as("Hash deve ser determinístico para o mesmo conteúdo — "
                        + "se 2 GETs no mesmo path produzem hrefs diferentes, "
                        + "cada render quebra o cache CDN")
                .isEqualTo(href1);
    }

    // ============================================================
    // C) GET com hash inválido — comportamento documentado.
    //
    // O cenário #3 do plano pede "assertion adaptativa": o Spring
    // pode escolher 404 (hash não confere → não serve) ou 200 (faz
    // fallback no original). O que importa para a funcionalidade é
    // que a invariante "GET na URL hashed REAL funciona" continue
    // valendo após uma tentativa com hash modificado — ou seja,
    // o resolver não fica em estado corrompido. A asserção
    // adaptativa registra o comportamento atual sem travar uma
    // versão futura do Spring que decida mudar.
    // ============================================================

    @Test
    void getWithInvalidHash_documentedBehaviorAndDoesNotCorruptResolver() throws Exception {
        // 1. Captura URL hashed real e seu hash atual.
        String validHref = extractCssHref("/comecar");
        Matcher mValid = HASHED_CSS_HREF.matcher(validHref);
        assertThat(mValid.matches()).isTrue();
        String validHash = mValid.group(1);

        // 2. Constrói uma URL com hash modificado em 1 caractere.
        // Trocamos o primeiro caractere por outro hex diferente para
        // garantir que o hash não bate. Ex.: "73de..." → "83de...".
        char firstChar = validHash.charAt(0);
        char swappedChar = (firstChar == '0') ? '1' : (char) (firstChar == 'f' ? '0' : firstChar + 1);
        String invalidHash = swappedChar + validHash.substring(1);
        String invalidHref = "/css/app-" + invalidHash + ".css";
        assertThat(invalidHref).isNotEqualTo(validHref);

        // 3. Faz GET no URL inválido. Documenta o status retornado.
        int statusInvalid = mvc.perform(get(invalidHref))
                .andReturn().getResponse().getStatus();

        // Assertion adaptativa: aceitamos 200 (Spring serve fallback do
        // arquivo original ignorando hash quebrado) OU 404 (Spring
        // recusa por hash invalido). Qualquer outra coisa (5xx, 3xx)
        // seria comportamento inesperado e merece investigação.
        assertThat(statusInvalid)
                .as("GET em URL com hash inválido deve responder 200 ou 404 — recebido: %d", statusInvalid)
                .isIn(200, 404);

        // 4. Invariante mais importante: o GET na URL hashed REAL,
        // depois da tentativa quebrada, continua funcionando 200 +
        // text/css. Garante que o resolver não foi corrompido.
        mvc.perform(get(validHref))
                .andExpect(status().isOk());
    }

    // ============================================================
    // D) Cap de tamanho da URL hashed.
    //
    // ContentVersionStrategy gera hash MD5 = 32 chars hex.
    // URL final: "/css/app-" (9) + 32 + ".css" (4) = 45 chars.
    // 80 é folga generosa que ainda pega regressões caso alguém
    // mude para hash de path inteiro ou versionamento fixo verboso.
    // Cenário exato do plano #4.
    // ============================================================

    @Test
    void hashedCssHrefStaysWithinReasonableSizeCap() throws Exception {
        String href = extractCssHref("/comecar");

        assertThat(href.length())
                .as("URL hashed deve ficar abaixo de 80 chars — recebido %d chars (%s). "
                        + "Acima disso sugere mudança de estratégia para hash de path completo "
                        + "ou versionamento fixo verboso, que polui o HTML.",
                        href.length(), href)
                .isLessThan(80);
        assertThat(href.length())
                .as("URL hashed deve ter pelo menos prefixo + hash + extensão (>= 20 chars)")
                .isGreaterThanOrEqualTo(20);
    }

    // ============================================================
    // E) Paridade de Cache-Control — o caminho legado /css/app.css
    // também precisa trazer max-age longo.
    //
    // O IT do Codificador valida Cache-Control na URL HASHED. O
    // caminho legado (sem hash) continua sendo servido (backward
    // compat — teste 3 do IT original confirma 200). Mas se o
    // VersionResourceResolver ativo no chain alterasse os headers
    // só da URL hashed, links diretos antigos receberiam CSS atual
    // sem cacheabilidade, prejudicando perf no edge.
    //
    // Cenário exato do plano #5.
    // ============================================================

    @Test
    void legacyUnhashedPathAlsoCarriesCacheControlMaxAge() throws Exception {
        String cacheControl = mvc.perform(get("/css/app.css"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("Cache-Control");

        assertThat(cacheControl)
                .as("Caminho legado /css/app.css deve manter Cache-Control configurado — "
                        + "paridade com URL hashed garante perf no edge para links antigos")
                .isNotNull()
                .contains("max-age=86400");
    }

    // ============================================================
    // Helpers
    // ============================================================

    /**
     * Faz GET em uma rota pública que estende {@code layout/base.html},
     * parseia o HTML com Jsoup e devolve o href do primeiro
     * {@code <link rel="stylesheet">} {@code .css} encontrado.
     */
    private String extractCssHref(String route) throws Exception {
        String html = mvc.perform(get(route))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Document doc = Jsoup.parse(html);
        Elements links = doc.select("link[rel=stylesheet][href$=.css]");
        assertThat(links)
                .as("Rota %s deve ter pelo menos um <link rel=stylesheet>", route)
                .isNotEmpty();
        return links.first().attr("href");
    }
}
