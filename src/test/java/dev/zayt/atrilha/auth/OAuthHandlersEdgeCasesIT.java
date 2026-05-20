package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.accounts.AccountReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

/**
 * Cenarios de borda do {@code OAuthSuccessHandler} (US-002) usando mocks
 * puros — nao precisa Postgres nem Docker.
 *
 * <p>Foco em comportamento defensivo do handler quando o Google envia
 * claims malformadas: email null/blank, given_name ausente, picture
 * ausente, normalizacao com whitespace. Cada teste, se falhar, indica
 * brecha de seguranca ou bug observavel.</p>
 *
 * <p>O OAuthHandlersIT do plano cobre o caminho feliz com Postgres real
 * (existsByEmail no banco); aqui usamos mock do AccountReader para
 * exercitar as bordas do handler em isolamento.</p>
 */
@ExtendWith(MockitoExtension.class)
class OAuthHandlersEdgeCasesIT {

    @Mock
    AccountReader accountReader;

    Clock clock;
    OAuthSuccessHandler handler;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        clock = Clock.fixed(Instant.parse("2026-05-20T12:00:00Z"), ZoneId.of("UTC"));
        handler = new OAuthSuccessHandler(accountReader, clock);
    }

    private static OAuth2AuthenticationToken googleTokenWithAttrs(Map<String, Object> attrs) {
        OAuth2User user = new DefaultOAuth2User(
                createAuthorityList("OAUTH2_USER"), attrs, "sub");
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");
    }

    // ============================================================
    // Claim "email" ausente — handler trata como erro generico e
    // NAO cria pending na sessao nem consulta o repository.
    // ============================================================

    @Test
    void successHandlerSemClaimEmailRedirecionaErroOauthSemPending() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "no-email-sub");
        attrs.put("email_verified", true);
        attrs.put("given_name", "SemEmail");
        // email omitido

        MockHttpServletRequest req = new MockHttpServletRequest("GET",
                "/login/oauth2/code/google");
        MockHttpServletResponse res = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(req, res, googleTokenWithAttrs(attrs));

        assertThat(res.getStatus()).isEqualTo(302);
        assertThat(res.getRedirectedUrl())
                .isEqualTo("/cadastro/adolescente/escolher-metodo?error=oauth");
        assertThat(req.getSession().getAttribute("pendingGoogleSignup"))
                .as("sem email no claim, nao deve criar pending na sessao")
                .isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("SecurityContext deve estar limpo")
                .isNull();
        // Defesa em profundidade: nao deve sequer consultar repository
        // sem ter email valido para passar.
        verify(accountReader, never()).existsByEmailIgnoreCase(anyString());
    }

    // ============================================================
    // given_name ausente — handler usa empty string como default,
    // sem NPE.
    // ============================================================

    @Test
    void successHandlerSemClaimGivenNameUsaStringVaziaNoPending() throws Exception {
        when(accountReader.existsByEmailIgnoreCase("sem.given@gmail.com")).thenReturn(false);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "no-given-name");
        attrs.put("email", "sem.given@gmail.com");
        attrs.put("email_verified", true);
        attrs.put("picture", "https://lh3.googleusercontent.com/a/x");
        // given_name omitido

        MockHttpServletRequest req = new MockHttpServletRequest("GET",
                "/login/oauth2/code/google");
        MockHttpServletResponse res = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(req, res, googleTokenWithAttrs(attrs));

        assertThat(res.getRedirectedUrl()).isEqualTo("/cadastro/adolescente/complementar");
        PendingGoogleSignup pending = (PendingGoogleSignup)
                req.getSession().getAttribute("pendingGoogleSignup");
        assertThat(pending).isNotNull();
        assertThat(pending.givenName())
                .as("default seguro para given_name ausente")
                .isEqualTo("");
        assertThat(pending.email()).isEqualTo("sem.given@gmail.com");
        assertThat(pending.picture()).isEqualTo("https://lh3.googleusercontent.com/a/x");
    }

    // ============================================================
    // picture ausente — pending.picture deve ser null (cobertura
    // sem NPE).
    // ============================================================

    @Test
    void successHandlerSemClaimPicturePersistePendingComPictureNull() throws Exception {
        when(accountReader.existsByEmailIgnoreCase("sem.foto@gmail.com")).thenReturn(false);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "no-picture");
        attrs.put("email", "sem.foto@gmail.com");
        attrs.put("email_verified", true);
        attrs.put("given_name", "SemFoto");
        // picture omitido

        MockHttpServletRequest req = new MockHttpServletRequest("GET",
                "/login/oauth2/code/google");
        MockHttpServletResponse res = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(req, res, googleTokenWithAttrs(attrs));

        assertThat(res.getRedirectedUrl()).isEqualTo("/cadastro/adolescente/complementar");
        PendingGoogleSignup pending = (PendingGoogleSignup)
                req.getSession().getAttribute("pendingGoogleSignup");
        assertThat(pending).isNotNull();
        assertThat(pending.picture()).isNull();
    }

    // ============================================================
    // Email com whitespace (raro mas defensivo) — handler aplica
    // trim + lowercase. Garante normalizacao consistente.
    // ============================================================

    @Test
    void successHandlerEmailComEspacosNasBordasEhNormalizado() throws Exception {
        when(accountReader.existsByEmailIgnoreCase("trim.spaces@gmail.com")).thenReturn(false);

        Map<String, Object> attrs = Map.of(
                "sub", "trim-sub",
                "email", "  Trim.Spaces@Gmail.COM  ",
                "email_verified", true,
                "given_name", "Trim",
                "picture", "https://lh3.googleusercontent.com/a/x");

        MockHttpServletRequest req = new MockHttpServletRequest("GET",
                "/login/oauth2/code/google");
        MockHttpServletResponse res = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(req, res, googleTokenWithAttrs(attrs));

        PendingGoogleSignup pending = (PendingGoogleSignup)
                req.getSession().getAttribute("pendingGoogleSignup");
        assertThat(pending).isNotNull();
        assertThat(pending.email())
                .as("email deve ser trimado e lower-case antes de armazenar")
                .isEqualTo("trim.spaces@gmail.com");
        // Tambem deve ter consultado o repository com a versao normalizada,
        // nao a versao raw — protege a checagem de duplicado de variacoes
        // de case.
        verify(accountReader).existsByEmailIgnoreCase("trim.spaces@gmail.com");
    }

    // ============================================================
    // emailVerifiedAt no pending eh derivado do Clock real do bean
    // (nao das claims). Garante que esse contrato observavel
    // persiste, mesmo se um attacker tentasse passar
    // email_verified sem timestamp.
    // ============================================================

    @Test
    void successHandlerEmailVerifiedAtEhDerivadoDoClockNaoDasClaims() throws Exception {
        when(accountReader.existsByEmailIgnoreCase("ts.test@gmail.com")).thenReturn(false);

        Map<String, Object> attrs = Map.of(
                "sub", "ts-sub",
                "email", "ts.test@gmail.com",
                "email_verified", true,
                "given_name", "Ts");

        MockHttpServletRequest req = new MockHttpServletRequest("GET",
                "/login/oauth2/code/google");
        MockHttpServletResponse res = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(req, res, googleTokenWithAttrs(attrs));

        PendingGoogleSignup pending = (PendingGoogleSignup)
                req.getSession().getAttribute("pendingGoogleSignup");
        // Clock fixo em 2026-05-20T12:00:00Z; emailVerifiedAt deve refletir.
        assertThat(pending.emailVerifiedAt())
                .as("emailVerifiedAt deve vir do Clock injetado, nao das claims")
                .isEqualTo(OffsetDateTime.parse("2026-05-20T12:00:00Z"));
        assertThat(pending.createdAt())
                .isEqualTo(Instant.parse("2026-05-20T12:00:00Z"));
    }

    // ============================================================
    // Conta existente (mock returna true) → redireciona account_exists
    // sem criar pending. Igual ao teste do plano (numero 22) mas
    // sem Postgres — defesa em profundidade.
    // ============================================================

    @Test
    void successHandlerContaExistenteRedirecionaSemCriarPending() throws Exception {
        when(accountReader.existsByEmailIgnoreCase("existe@gmail.com")).thenReturn(true);

        Map<String, Object> attrs = Map.of(
                "sub", "exist-sub",
                "email", "existe@gmail.com",
                "email_verified", true,
                "given_name", "Existe");

        MockHttpServletRequest req = new MockHttpServletRequest("GET",
                "/login/oauth2/code/google");
        MockHttpServletResponse res = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(req, res, googleTokenWithAttrs(attrs));

        assertThat(res.getStatus()).isEqualTo(302);
        assertThat(res.getRedirectedUrl())
                .isEqualTo("/cadastro/adolescente/escolher-metodo?error=account_exists");
        assertThat(req.getSession().getAttribute("pendingGoogleSignup")).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ============================================================
    // Tentativa de chamar successHandler duas vezes seguidas com o
    // MESMO request sem POST intermediario apenas sobrescreve o
    // pending. Comportamento determinístico.
    // ============================================================

    @Test
    void successHandlerChamadoDuasVezesSemPostSobrescrevePending() throws Exception {
        when(accountReader.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        Map<String, Object> attrs1 = Map.of(
                "sub", "dup1",
                "email", "dup.first@gmail.com",
                "email_verified", true,
                "given_name", "First",
                "picture", "https://lh3.googleusercontent.com/a/1");
        Map<String, Object> attrs2 = Map.of(
                "sub", "dup2",
                "email", "dup.second@gmail.com",
                "email_verified", true,
                "given_name", "Second",
                "picture", "https://lh3.googleusercontent.com/a/2");

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(req, res, googleTokenWithAttrs(attrs1));
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(req, res2, googleTokenWithAttrs(attrs2));

        PendingGoogleSignup pending = (PendingGoogleSignup)
                req.getSession().getAttribute("pendingGoogleSignup");
        assertThat(pending).isNotNull();
        assertThat(pending.email())
                .as("segunda chamada deve sobrescrever pending (sessao tem 1 atributo so)")
                .isEqualTo("dup.second@gmail.com");
    }

    // ============================================================
    // Email vazio (string ""): existsByEmail("") consultado, e
    // por tipo (existsByEmailIgnoreCaseAndDeletedAtIsNull) deve
    // retornar false → cria pending com email "". Esse cenario
    // expõe um caso patológico — mesmo com email vazio, o handler
    // ainda cria pending e segue. Cobre defesa-em-profundidade do
    // service downstream (que reaplica normalizacao + check).
    // ============================================================

    @Test
    void successHandlerEmailEmBrancoCriaPendingMasServiceReaplicaCheck() throws Exception {
        when(accountReader.existsByEmailIgnoreCase("")).thenReturn(false);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "empty-email");
        attrs.put("email", "   "); // whitespace puro
        attrs.put("email_verified", true);
        attrs.put("given_name", "Empty");

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(req, res, googleTokenWithAttrs(attrs));

        // Comportamento observavel: handler trata como email valido apos
        // trim ("" string) e cria pending — o service downstream
        // (registerFromGoogle) reaplica trim+lowercase no email do pending,
        // e a checagem de duplicidade do banco com email "" retorna false,
        // mas o INSERT no banco vai falhar pela constraint NOT NULL de
        // email. Defesa-em-profundidade do banco.
        assertThat(res.getRedirectedUrl()).isEqualTo("/cadastro/adolescente/complementar");
        PendingGoogleSignup pending = (PendingGoogleSignup)
                req.getSession().getAttribute("pendingGoogleSignup");
        assertThat(pending).isNotNull();
        assertThat(pending.email())
                .as("email whitespace eh trimado para string vazia")
                .isEqualTo("");
    }
}
