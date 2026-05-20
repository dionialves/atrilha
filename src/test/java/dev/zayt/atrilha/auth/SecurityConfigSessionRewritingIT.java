package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.AtrilhaApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regressão de segurança após habilitação de
 * {@code sessionManagement(s -> s.enableSessionUrlRewriting(true))} em
 * {@link SecurityConfig} pela fix-001.
 *
 * <p>Cenário 6 do plano de teste da issue #49: garantir que a remoção
 * do {@code DisableEncodeUrlFilter} do chain — necessária para destravar
 * o {@code ResourceUrlEncodingFilter} do Spring Web — não afrouxa as
 * outras camadas de proteção que a app depende:
 * <ul>
 *   <li>Rotas autenticadas (ex.: {@code GET /verificar-email}) sem
 *       sessão continuam bloqueadas (302/401/403).</li>
 *   <li>POST em rota com CSRF habilitado (ex.:
 *       {@code POST /verificar-email/reenviar} e
 *       {@code POST /cadastro/adolescente}) sem token CSRF continuam
 *       respondendo 403 Forbidden.</li>
 * </ul>
 *
 * <p>Estes testes correm no profile {@code test} default (chain de
 * resources desligado), porque o efeito que estamos validando vem
 * <em>exclusivamente</em> do {@code SecurityConfig} global —
 * independente de a fingerprint de CSS estar ativa ou não.</p>
 */
@SpringBootTest(classes = AtrilhaApplication.class)
@ActiveProfiles("test")
class SecurityConfigSessionRewritingIT {

    @Autowired
    WebApplicationContext ctx;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    // ============================================================
    // A) Rotas autenticadas sem sessão continuam bloqueadas.
    //
    // O contrato existente (EmailVerificationControllerIT
    // #getVerificarEmail_anonymous_isUnauthorized) aceita
    // 3xx/401/403 — qualquer um indica bloqueio. Reaplicamos esse
    // contrato aqui na ótica da fix-001: a mudança no SecurityConfig
    // (enableSessionUrlRewriting=true) só afeta a presença do
    // DisableEncodeUrlFilter, NÃO o authorizeHttpRequests. Se essa
    // invariante quebrar, é regressão de segurança.
    // ============================================================

    @Test
    void getVerificarEmailWithoutSessionIsStillBlocked() throws Exception {
        int status = mvc.perform(get("/verificar-email"))
                .andReturn().getResponse().getStatus();

        assertThat(status)
                .as("GET /verificar-email anônimo deve continuar bloqueado (3xx/401/403) "
                        + "após enableSessionUrlRewriting(true) — status recebido: %d", status)
                .satisfiesAnyOf(
                        s -> assertThat(s).isBetween(300, 399),
                        s -> assertThat(s).isEqualTo(401),
                        s -> assertThat(s).isEqualTo(403)
                );
    }

    @Test
    void getVerificarEmailReenviarWithoutSessionIsStillBlocked() throws Exception {
        // Rota também listada como .authenticated() no SecurityConfig.
        // Mesma invariante de bloqueio anônimo aplica.
        int status = mvc.perform(get("/verificar-email/reenviar"))
                .andReturn().getResponse().getStatus();

        assertThat(status)
                .as("GET /verificar-email/reenviar anônimo deve continuar bloqueado — recebido: %d", status)
                .satisfiesAnyOf(
                        s -> assertThat(s).isBetween(300, 399),
                        s -> assertThat(s).isEqualTo(401),
                        s -> assertThat(s).isEqualTo(403),
                        // Spring Web pode rejeitar GET em rota que aceita POST com 405 antes do
                        // filter chain processar autenticação. Aceitamos como bloqueio.
                        s -> assertThat(s).isEqualTo(405)
                );
    }

    // ============================================================
    // B) POST sem CSRF token continua 403 — contrato de CSRF
    // preservado.
    //
    // CSRF é ortogonal ao DisableEncodeUrlFilter. enableSessionUrlRewriting
    // mexe apenas em url-rewriting de sessão, não em
    // CsrfConfigurer.csrfTokenRepository. Mas como a fix-001 alterou o
    // builder do SecurityFilterChain, é prudente verificar que o
    // CsrfFilter ainda está no chain e ainda bloqueia POST sem token.
    //
    // Testamos POST em DUAS rotas distintas (pública e autenticada)
    // para confirmar que o CSRF é universal:
    // - POST /cadastro/adolescente (rota pública) sem CSRF → 403
    // - POST /verificar-email/reenviar (rota autenticada) sem
    //   CSRF → 403 (CsrfFilter roda antes do AuthorizationFilter,
    //   então sem token retorna 403 antes mesmo da checagem de
    //   sessão)
    // ============================================================

    @Test
    void postPublicRouteWithoutCsrfIsStillForbidden() throws Exception {
        // POST em /cadastro/adolescente sem _csrf — CSRF é universal,
        // mesmo em rota .permitAll(). enableSessionUrlRewriting(true)
        // não pode ter desabilitado o CsrfFilter por efeito colateral.
        mvc.perform(post("/cadastro/adolescente")
                        .param("email", "qa-csrf-check@example.com")
                        .param("password", "Senha@Forte123")
                        .param("displayName", "QA"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postAuthenticatedRouteWithoutCsrfIsStillForbidden() throws Exception {
        // POST em rota autenticada sem CSRF → 403 do CsrfFilter,
        // não 401/302 da autenticação. CsrfFilter está mais à frente
        // no chain, então responde antes mesmo de o
        // AuthorizationFilter checar sessão. Esse é o contrato
        // pré-existente que precisamos preservar.
        mvc.perform(post("/verificar-email/reenviar"))
                .andExpect(status().isForbidden());
    }
}
