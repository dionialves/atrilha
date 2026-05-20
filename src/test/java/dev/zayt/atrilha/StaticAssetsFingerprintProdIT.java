package dev.zayt.atrilha;

import jakarta.servlet.Filter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato do fingerprint de assets estaticos (fix-001). Liga a
 * estrategia content-hash que vale em prod, sem alterar o profile
 * `test` global — isolamento via @TestPropertySource.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.web.resources.chain.enabled=true",
    "spring.web.resources.chain.strategy.content.enabled=true",
    "spring.web.resources.chain.strategy.content.paths=/**",
    "spring.web.resources.cache.cachecontrol.max-age=86400"
})
class StaticAssetsFingerprintProdIT {

    @Autowired WebApplicationContext ctx;
    MockMvc mvc;

    // Regex que casa /css/app-<hash>.css onde <hash> e hex de 8+ chars.
    // FileNameVersionPathStrategy.addVersion() usa hifen como separador
    // (AbstractVersionStrategy.java:144 — "baseFilename + '-' + version + '.' + extension").
    // Content-version do Spring usa MD5 (32 chars hex). 8+ e tolerante.
    private static final Pattern HASHED_CSS_HREF =
        Pattern.compile("/css/app-([a-f0-9]{8,})\\.css");

    @BeforeEach
    void setUp() {
        // MockMvcBuilders.webAppContextSetup nao carrega automaticamente os
        // FilterRegistrationBeans do contexto Spring Boot — diferente de um
        // Tomcat embedded em prod. Como o ResourceUrlEncodingFilter e
        // registrado via FilterRegistrationBean pelo ThymeleafAutoConfiguration
        // (condicional em spring.web.resources.chain.enabled=true), precisamos
        // adiciona-lo explicitamente ao chain de filtros do MockMvc para
        // reproduzir o comportamento prod-like que o teste valida.
        Filter resourceUrlEncodingFilter = resolveResourceUrlEncodingFilter();
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
            .addFilters(resourceUrlEncodingFilter)
            .apply(springSecurity())
            .build();
    }

    private Filter resolveResourceUrlEncodingFilter() {
        // Em runtime prod o filter sai do FilterRegistrationBean<ResourceUrlEncodingFilter>
        // criado em ThymeleafAutoConfiguration.ResourceUrlEncodingFilterConfiguration.
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

    @Test
    void cssLinkInRenderedHtmlIsHashed() throws Exception {
        String html = mvc.perform(get("/comecar"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Document doc = Jsoup.parse(html);
        Elements links = doc.select("link[rel=stylesheet][href$=.css]");
        assertThat(links).as("Deve existir pelo menos um <link rel=stylesheet>").isNotEmpty();

        String href = links.first().attr("href");
        Matcher m = HASHED_CSS_HREF.matcher(href);
        assertThat(m.matches())
            .as("href do CSS deve casar /css/app-<hash>.css — recebido: %s", href)
            .isTrue();
    }

    @Test
    void hashedCssUrlReturns200AndCssContent() throws Exception {
        String hashedHref = extractCssHref();
        assertThat(HASHED_CSS_HREF.matcher(hashedHref).matches()).isTrue();

        String body = mvc.perform(get(hashedHref))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/css")))
            .andReturn().getResponse().getContentAsString();

        assertThat(body)
            .as("Conteudo servido na URL hashed deve ser o CSS Tailwind compilado")
            .contains(":root")
            .contains("--color-primary-500");
    }

    @Test
    void unhashedCssUrlAlsoReturns200ForBackwardCompat() throws Exception {
        mvc.perform(get("/css/app.css"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/css")));
    }

    @Test
    void cssResponseHasCacheControlMaxAge() throws Exception {
        String hashedHref = extractCssHref();

        String cacheControl = mvc.perform(get(hashedHref))
            .andExpect(status().isOk())
            .andReturn().getResponse().getHeader("Cache-Control");

        assertThat(cacheControl)
            .as("CSS hashed deve ser cacheavel longo prazo — content-hash garante invalidacao por URL")
            .isNotNull()
            .contains("max-age=86400");
    }

    /**
     * Helper: faz GET em uma pagina publica que estende layout/base.html,
     * parseia o HTML com Jsoup e devolve o href do primeiro
     * <link rel="stylesheet"> .css encontrado.
     */
    private String extractCssHref() throws Exception {
        String html = mvc.perform(get("/comecar"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        Document doc = Jsoup.parse(html);
        Elements links = doc.select("link[rel=stylesheet][href$=.css]");
        assertThat(links).as("Deve existir pelo menos um <link rel=stylesheet>").isNotEmpty();
        return links.first().attr("href");
    }
}
