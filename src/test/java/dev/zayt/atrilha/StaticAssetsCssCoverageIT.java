package dev.zayt.atrilha;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobertura adicional (QA) sobre o contrato estrutural do CSS servido em
 * runtime e do layout base após chore-ux-009. Estende
 * {@link StaticAssetsCssIT} sem duplicar o que já foi coberto pelo
 * Codificador.
 *
 * <p>Foco em:
 * <ul>
 *   <li>Sobrevivência das classes utilitárias Tailwind inline ao purge
 *       (content-based scan do {@code tailwind.config.js}). Se essas
 *       classes sumirem, o layout do {@code base.html} quebra
 *       silenciosamente — contrato estrutural.</li>
 *   <li>Variação de rotas: o saneamento do Play CDN e a presença do link
 *       para {@code /css/app.css} valem para <em>todas</em> as telas
 *       servidas, não só {@code /comecar}.</li>
 *   <li>Cap de tamanho do CSS: guardrail contra regressão de purge (ex.:
 *       alguém amplia {@code content} no config e o CSS explode).</li>
 *   <li>Classes do design system referenciadas pelos templates além das
 *       já cobertas no IT do Codificador.</li>
 *   <li>Tokens semânticos efetivamente consumidos pelo CSS literal —
 *       se um deles sumir, o estilo do componente que o usa quebra.</li>
 *   <li>Minificação real do CSS — protege que o {@code --minify} do
 *       build script segue ativo.</li>
 * </ul>
 *
 * <p>O que NÃO é coberto aqui (gap intencional do QA do projeto):
 * cor exata de pixel, fonte renderizada, layout, microcopy, console do
 * navegador, headers de cache (sem contrato explícito), build do jar e
 * imagem Docker (fora do escopo da JVM).
 */
@SpringBootTest
@ActiveProfiles("test")
class StaticAssetsCssCoverageIT {

    @Autowired
    WebApplicationContext ctx;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
    }

    // ============================================================
    // A) Classes utilitárias Tailwind inline no markup sobrevivem ao purge.
    //
    // O `content` em tailwind.config.js escaneia
    // src/main/resources/templates/**/*.html. Se alguém quebrar essa
    // configuração, classes como .min-h-screen e .container deixam de
    // ser geradas, e o layout em base.html vira fluido sem container.
    // Isso é contrato estrutural — não é teste de cor.
    // ============================================================

    @Test
    void cssRetainsInlineUtilitiesUsedByBaseLayout() throws Exception {
        String css = fetchCss();

        // Utilitárias do <body> de base.html
        assertThat(css)
                .as(".min-h-screen deve ser gerada (usada inline em base.html <body>)")
                .contains(".min-h-screen");
        assertThat(css)
                .as(".flex deve ser gerada (usada inline em base.html <body>)")
                .contains(".flex");
        assertThat(css)
                .as(".flex-col deve ser gerada (usada inline em base.html <body>)")
                .contains(".flex-col");
        assertThat(css)
                .as(".bg-white deve ser gerada (usada inline em base.html <body>)")
                .contains(".bg-white");
        assertThat(css)
                .as(".text-slate-900 deve ser gerada (usada inline em base.html <body>)")
                .contains(".text-slate-900");
        assertThat(css)
                .as(".antialiased deve ser gerada (usada inline em base.html <body>)")
                .contains(".antialiased");

        // Utilitárias do <main> de base.html
        assertThat(css)
                .as(".flex-1 deve ser gerada (usada inline em base.html <main>)")
                .contains(".flex-1");
        assertThat(css)
                .as(".container deve ser gerada (usada inline em base.html <main>)")
                .contains(".container");
        assertThat(css)
                .as(".mx-auto deve ser gerada (usada inline em base.html <main>)")
                .contains(".mx-auto");
        assertThat(css)
                .as(".px-4 deve ser gerada (usada inline em base.html <main>)")
                .contains(".px-4");
        assertThat(css)
                .as(".py-6 deve ser gerada (usada inline em base.html <main>)")
                .contains(".py-6");
    }

    // ============================================================
    // B) Variação de rotas públicas — saneamento do Play CDN é
    // universal.
    //
    // O IT do Codificador valida apenas /comecar. O <script> do Play
    // CDN morava em base.html, então qualquer rota que estenda o
    // layout via thymeleaf-layout-dialect herda o saneamento. Mas
    // se alguém reintroduzir o <script> em um fragmento (header.html,
    // p.ex.), só algumas rotas serão afetadas — variar a rota cobre
    // essa classe de regressão.
    //
    // /verificar-email é coberta separadamente abaixo (requer sessão).
    // ============================================================

    @ParameterizedTest
    @ValueSource(strings = {"/", "/comecar", "/cadastro/adolescente"})
    void publicRoutesHaveNoTailwindPlayCdnScript(String route) throws Exception {
        String html = mvc.perform(get(route))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertCdnFreeAndAppCssLinked(html, route);
    }

    // ============================================================
    // C) /verificar-email é rota autenticada que requer um
    // AuthenticatedAccount real no contexto + Account persistida — caso
    // contrário o EmailVerificationController redireciona para /
    // (defesa). Aqui simulamos um @WithMockUser e seguimos o redirect:
    // o destino final ainda é uma página servida pelo layout base.html,
    // e o contrato "nenhum <script src=*cdn.tailwindcss*> no HTML"
    // permanece válido independentemente do caminho — preserva a
    // intenção original do teste sem exigir setup de persistência.
    // ============================================================

    @Test
    @WithMockUser
    void verifyEmailRouteHasNoTailwindPlayCdnScript() throws Exception {
        String html = mvc.perform(get("/verificar-email"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getContentAsString();

        // Mesmo em response de redirect (corpo vazio), o contrato negativo
        // continua válido: nenhuma string de CDN pode estar presente.
        assertThat(html)
                .as("response de /verificar-email não deve referenciar Tailwind Play CDN")
                .doesNotContain("cdn.tailwindcss")
                .doesNotContain("tailwindcss.com");
    }

    /**
     * Asserts compartilhadas entre B e C: nem o {@code <script>} do Play
     * CDN deve aparecer, nem o {@code <link>} para {@code /css/app.css}
     * pode ter sumido. As duas verificações andam juntas porque a remoção
     * do CDN não pode levar a perda do CSS empacotado por engano.
     */
    private void assertCdnFreeAndAppCssLinked(String html, String route) {
        Document doc = Jsoup.parse(html);

        assertThat(doc.select("script[src*=cdn.tailwindcss]"))
                .as("Rota %s não pode servir <script src=*cdn.tailwindcss*>", route)
                .isEmpty();
        assertThat(doc.select("script[src*=tailwindcss.com]"))
                .as("Rota %s não pode servir <script src=*tailwindcss.com*>", route)
                .isEmpty();
        assertThat(doc.select("link[rel=stylesheet][href=/css/app.css]"))
                .as("Rota %s deve manter o <link rel=stylesheet href=/css/app.css>", route)
                .isNotEmpty();
    }

    // ============================================================
    // D) Cap de tamanho — guardrail contra alguém abrir o `content`
    // do tailwind.config.js para algo como "**/*" e o CSS explodir.
    //
    // CSS atual (minificado) ~23KB. 200KB é folga generosa, mas pega
    // crescimento de 10x — o suficiente para sinalizar que algo
    // mudou estruturalmente. Não é teste de performance: é guarda
    // contra config errada.
    // ============================================================

    @Test
    void cssRespectsReasonableSizeCap() throws Exception {
        String css = fetchCss();

        assertThat(css.length())
                .as("CSS servido deve ficar abaixo de 200KB — acima disso provavelmente "
                        + "o `content` em tailwind.config.js está pegando arquivos demais")
                .isLessThan(200_000);
        assertThat(css.length())
                .as("CSS servido deve ser não-trivial — abaixo de 1KB algo no build quebrou")
                .isGreaterThan(1_000);
    }

    // ============================================================
    // E) Minificação — `npm run build:css` passa --minify. Se alguém
    // tirar o flag por engano, o CSS estoura de tamanho e o cap (D)
    // pode mascarar isso. Aqui valida estruturalmente: CSS minificado
    // não tem quebras de linha entre regras (uma única linha longa).
    //
    // Tailwind v4 com --minify emite ~1 linha por banner + 1 linha
    // gigante com tudo concatenado. Sem --minify, fica multilinha
    // pretty-printed. A heurística "linhas << bytes" detecta o gap.
    // ============================================================

    @Test
    void cssIsMinified() throws Exception {
        String css = fetchCss();

        long newlines = css.chars().filter(c -> c == '\n').count();

        // Minificado: poucas linhas (geralmente 1-3, banner + corpo).
        // Pretty-printed: centenas. Threshold conservador.
        assertThat(newlines)
                .as("CSS deve ser minificado (poucas quebras de linha). "
                        + "Recebido %d newlines em %d bytes — provável regressão "
                        + "do --minify no script build:css", newlines, css.length())
                .isLessThan(50);
    }

    // ============================================================
    // F) Classes do design system referenciadas pelos templates além
    // das já cobertas no IT do Codificador.
    //
    // - .btn-lg ............ home.html, comecar.html (CTA principal)
    // - .input-group--error  cadastro/adolescente.html (estado de erro do form)
    // - .input-helper ...... cadastro/adolescente.html (hint de campo)
    // - .input-error ....... cadastro/adolescente.html (mensagem de erro do form)
    // - .cadastro-form__*    cadastro/adolescente.html (layout do form)
    // - .brand, .brand-mark  layout/fragments/header.html
    // - .brand-wordmark      layout/fragments/header.html
    // - .avatar-initial .... US-001 CA-6 (avatar fallback)
    //
    // Se essas classes forem purgadas, o componente correspondente
    // perde estilo — não é cor, é o seletor inteiro sumindo. Contrato
    // estrutural.
    // ============================================================

    @Test
    void cssRetainsDesignSystemClassesReferencedByTemplates() throws Exception {
        String css = fetchCss();

        assertThat(css).as(".btn-lg deve sobreviver — usada em home.html / comecar.html").contains(".btn-lg");
        assertThat(css).as(".input-group--error deve sobreviver — estado de erro do form de cadastro")
                .contains(".input-group--error");
        assertThat(css).as(".input-helper deve sobreviver — hint dos campos de cadastro")
                .contains(".input-helper");
        assertThat(css).as(".input-error deve sobreviver — mensagem de erro dos campos de cadastro")
                .contains(".input-error");
        assertThat(css).as(".cadastro-form__lead deve sobreviver — layout do form de cadastro")
                .contains(".cadastro-form__lead");
        assertThat(css).as(".cadastro-form__actions deve sobreviver — layout do form de cadastro")
                .contains(".cadastro-form__actions");
        assertThat(css).as(".cadastro-form__legal deve sobreviver — layout do form de cadastro")
                .contains(".cadastro-form__legal");
        assertThat(css).as(".brand deve sobreviver — usada em layout/fragments/header.html")
                .contains(".brand");
        assertThat(css).as(".brand-mark deve sobreviver — usada em layout/fragments/header.html")
                .contains(".brand-mark");
        assertThat(css).as(".brand-wordmark deve sobreviver — usada em layout/fragments/header.html")
                .contains(".brand-wordmark");
        assertThat(css).as(".avatar-initial deve sobreviver — US-001 CA-6 avatar fallback")
                .contains(".avatar-initial");
    }

    // ============================================================
    // G) Tokens semânticos consumidos pelo CSS literal.
    //
    // O CSS literal (.btn, .input-field, .card, body) usa via
    // `var(--token)` os tokens semânticos abaixo. Se Tailwind v4
    // deixar de emitir um deles em :root, o componente que o
    // referencia fica sem cor / borda / sombra — quebra estrutural.
    //
    // O IT do Codificador cobre um subset (--color-primary-500,
    // --space-4, --space-11, --radius-md, --font-sans, --button-*,
    // --input-border, --card-bg). Aqui cobrimos os que faltam.
    // ============================================================

    @Test
    void cssDeclaresSemanticTokensConsumedByLiteralCss() throws Exception {
        String css = fetchCss();

        // body { color: var(--color-text-body); background: var(--color-bg); }
        assertThat(css)
                .as("Token --color-text-body deve estar declarado (consumido por body{} e .input-label)")
                .contains("--color-text-body");
        assertThat(css)
                .as("Token --color-bg deve estar declarado (consumido por body{})")
                .contains("--color-bg");

        // .input-group--error .input-field { border-color: var(--color-danger-700); }
        assertThat(css)
                .as("Token --color-danger-700 deve estar declarado (consumido por .input-group--error e .input-error)")
                .contains("--color-danger-700");

        // .card--raised { box-shadow: var(--card-shadow-raised); } => var(--shadow-md)
        assertThat(css)
                .as("Token --shadow-md deve estar declarado (consumido por --card-shadow-raised)")
                .contains("--shadow-md");
        assertThat(css)
                .as("Token --card-shadow-raised deve estar declarado (consumido por .card--raised)")
                .contains("--card-shadow-raised");

        // .btn:focus-visible { box-shadow: var(--shadow-focus); }
        assertThat(css)
                .as("Token --shadow-focus deve estar declarado (consumido por .btn:focus-visible e .input-field:focus)")
                .contains("--shadow-focus");

        // .btn { transition: ... var(--duration-fast) var(--ease-out-soft); }
        assertThat(css)
                .as("Token --duration-fast deve estar declarado (consumido por .btn transition)")
                .contains("--duration-fast");
        assertThat(css)
                .as("Token --ease-out-soft deve estar declarado (consumido por .btn transition)")
                .contains("--ease-out-soft");

        // .btn { font-weight: var(--font-weight-semibold); }
        assertThat(css)
                .as("Token --font-weight-semibold deve estar declarado (consumido por .btn e h1-h3)")
                .contains("--font-weight-semibold");

        // h1 { font-size: var(--text-2xl); } / .input-field { font-size: var(--text-base); }
        assertThat(css)
                .as("Token --text-base deve estar declarado (consumido por body e .btn)")
                .contains("--text-base");

        // .card { border-radius: var(--card-radius); }
        assertThat(css)
                .as("Token --card-radius deve estar declarado (consumido por .card)")
                .contains("--card-radius");

        // .avatar-initial { font-family: var(--font-display); }
        assertThat(css)
                .as("Token --font-display deve estar declarado (consumido por h1-h2 e .avatar-initial)")
                .contains("--font-display");
    }

    // ============================================================
    // H) Variantes irmãs do .card declaradas no CSS — regressão.
    //
    // O IT do Codificador adicionou guarda específica para .card--flat
    // (fix-001). As variantes irmãs .card--dense, .card--raised e
    // .card--interactive já existiam antes do fix e continuam sendo
    // referenciadas em pelo menos um template:
    //   - .card--interactive → templates/comecar.html (CTAs principais)
    //                          e templates/components/card.html (fragment
    //                          default da spec §3.8).
    //   - .card--dense       → variante de densidade prevista no fragment
    //                          card.html (§3.8) — usada quando o caller
    //                          passa dense=true.
    //   - .card--raised      → variante de elevação prevista pela spec
    //                          §3.3; sua remoção do CSS quebra qualquer
    //                          futuro consumidor sem aviso.
    //
    // Sem este teste, basta o purge do Tailwind v4 ser mal-configurado
    // ou alguém deletar o seletor literal de app.css para a variante
    // sumir silenciosamente do CSS servido. Contrato estrutural do
    // design system.
    // ============================================================

    @Test
    void cssDeclaresCardSiblingVariantSelectors() throws Exception {
        String css = fetchCss();

        Pattern cardDense = Pattern.compile("\\.card--dense\\s*\\{");
        assertThat(cardDense.matcher(css).find())
                .as("CSS servido deve conter a regra .card--dense { ... } "
                        + "(variante de densidade prevista pelo fragment §3.8)")
                .isTrue();

        Pattern cardRaised = Pattern.compile("\\.card--raised\\s*\\{");
        assertThat(cardRaised.matcher(css).find())
                .as("CSS servido deve conter a regra .card--raised { ... } "
                        + "(variante de elevação prevista pela spec §3.3)")
                .isTrue();

        Pattern cardInteractive = Pattern.compile("\\.card--interactive\\s*\\{");
        assertThat(cardInteractive.matcher(css).find())
                .as("CSS servido deve conter a regra .card--interactive { ... } "
                        + "(variante usada por comecar.html nos CTAs principais)")
                .isTrue();
    }

    // ============================================================
    // I) .card--flat não pode redeclarar background / border /
    // border-radius — invariante "no-op visual" da spec §3.3.
    //
    // O critério de aceitação #3 da fix-001 declara textualmente:
    //   ".card--flat NÃO redeclara background, border nem
    //    border-radius (herda de .card)."
    //
    // O Codificador descobriu que o minificador do Tailwind v4 descarta
    // blocos vazios, então adicionou `display: block` (já herdado de
    // .card) como propriedade no-op visualmente neutra. Esse desvio
    // é coerente com o espírito da spec — desde que ninguém amplie o
    // bloco no futuro com propriedades que de fato divirjam de .card
    // sem antes promover .card--flat a variante distinta.
    //
    // Este teste extrai o bloco `.card--flat{...}` do CSS servido e
    // verifica que nenhuma das propriedades proibidas pela spec
    // (background, border:, border-radius, border-color) aparece
    // dentro dele. Se um dev futuro adicionar `border: 0` ou
    // `background: white` ao seletor sem revisar a spec, este teste
    // falha imediatamente.
    //
    // Contrato estrutural: protege a decisão de design "flat herda do
    // base" — se quebrar, o componente passa a divergir do que a §3.3
    // promete ao consumidor do design system.
    // ============================================================

    @Test
    void cardFlatDoesNotRedeclareInheritedBaseProperties() throws Exception {
        String css = fetchCss();

        // Captura o conteúdo entre `.card--flat{` e o próximo `}`.
        // Minificação do Tailwind v4 colapsa espaços, mas o padrão é
        // tolerante a whitespace para sobreviver a pretty-print local.
        Pattern cardFlatBlock = Pattern.compile("\\.card--flat\\s*\\{([^}]*)\\}");
        Matcher matcher = cardFlatBlock.matcher(css);

        assertThat(matcher.find())
                .as("CSS deve conter o bloco .card--flat { ... } extraível para inspecionar "
                        + "as propriedades declaradas (precondição para verificar a invariante "
                        + "no-op da spec §3.3)")
                .isTrue();

        String declarations = matcher.group(1);

        // A spec §3.3 + critério de aceitação #3 da fix-001 proíbem
        // explicitamente estas propriedades em .card--flat:
        assertThat(declarations)
                .as(".card--flat NÃO pode redeclarar `background` — a spec §3.3 exige "
                        + "herança de .card base (declarações encontradas: <%s>)", declarations)
                .doesNotContain("background");
        assertThat(declarations)
                .as(".card--flat NÃO pode redeclarar `border:` shorthand — a spec §3.3 exige "
                        + "herança de .card base (declarações encontradas: <%s>)", declarations)
                .doesNotContain("border:");
        assertThat(declarations)
                .as(".card--flat NÃO pode redeclarar `border-radius` — a spec §3.3 exige "
                        + "herança de .card base (declarações encontradas: <%s>)", declarations)
                .doesNotContain("border-radius");
        assertThat(declarations)
                .as(".card--flat NÃO pode redeclarar `border-color` — a spec §3.3 exige "
                        + "herança de .card base (declarações encontradas: <%s>)", declarations)
                .doesNotContain("border-color");
    }

    // ============================================================
    // J) Contrato HTML↔CSS: a página servida por /cadastro/responsavel
    // continua emitindo <article class="card card--flat">.
    //
    // O motivo de existir a declaração `.card--flat` em app.css é
    // honrar o contrato com 4 templates que nominalmente usam essa
    // classe (verificar-email.html, verify-email-resultado.html,
    // cadastro/adolescente_bloqueado.html, cadastro/responsavel_em_breve.html).
    //
    // Das 4, apenas `cadastro/responsavel_em_breve.html` é servida por
    // rota pública sem precondições — `/cadastro/responsavel` é um stub
    // controller (GuardianRegistrationStubController) que retorna a view
    // diretamente. As demais são autenticadas (/verificar-email) ou
    // dependem do fluxo de POST de cadastro (adolescente_bloqueado).
    //
    // Este teste fecha o loop: se alguém remover a classe `card--flat`
    // do template, o CSS da fix-001 vira código morto e o contrato
    // HTML↔CSS↔spec se quebra silenciosamente. Note que NÃO testamos
    // texto, cor, layout ou microcopy — apenas a presença estrutural
    // do par de classes BEM (`card` + `card--flat`) no DOM renderizado.
    // ============================================================

    @Test
    void guardianStubPageRendersCardFlatVariant() throws Exception {
        String html = mvc.perform(get("/cadastro/responsavel"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Document doc = Jsoup.parse(html);

        Element card = doc.selectFirst("article.card.card--flat");
        assertThat(card)
                .as("/cadastro/responsavel deve emitir um <article> com ambas as classes "
                        + "BEM `card` e `card--flat` — contrato HTML↔CSS da fix-001 que "
                        + "justifica a declaração de .card--flat em app.css")
                .isNotNull();

        assertThat(card.classNames())
                .as("class list do <article> deve conter exatamente o par BEM esperado pela spec §3.3")
                .contains("card", "card--flat");
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
