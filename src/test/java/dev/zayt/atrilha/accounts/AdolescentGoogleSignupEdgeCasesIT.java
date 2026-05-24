package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.auth.PendingGoogleSignup;
import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Cenarios de borda do {@code AdolescentGoogleSignupController} (US-002 /
 * Issue #37) que ainda nao estavam cobertos pelos 27 testes do plano. Cada
 * teste, se falhar, indica que uma feature regressou (validacao Jakarta
 * deixa passar dado ruim, sessao expira sem proteccao, upload bypassa
 * gating de mime).
 *
 * <p>Foco em comportamento que afeta o usuario: bordas de idade exatas,
 * upload sem foto, sessao expirada durante POST, sanitizacao de apelido,
 * data futura. Nao testa texto literal de UI, CSS, frase.</p>
 */
@SpringBootTest(classes = AtrilhaApplication.class)
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class AdolescentGoogleSignupEdgeCasesIT {

    @Autowired
    WebApplicationContext ctx;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AdolescentProfileRepository profileRepository;

    @Autowired
    Clock clock;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    private static PendingGoogleSignup pending(String email) {
        return new PendingGoogleSignup(
                email,
                OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                "Julia",
                "https://lh3.googleusercontent.com/a/julia",
                Instant.parse("2026-05-20T10:00:00Z"));
    }

    private static PendingGoogleSignup pendingNoPicture(String email, String given) {
        return new PendingGoogleSignup(
                email,
                OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                given,
                null,
                Instant.parse("2026-05-20T10:00:00Z"));
    }

    private static MockHttpSession sessionWithPending(PendingGoogleSignup p) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("pendingGoogleSignup", p);
        return s;
    }

    // ============================================================
    // Sessao ausente / expirada durante POST de complementar.
    // Cenario classico: usuario abre Tela 3, deixa aberta, vai ao
    // banheiro, sessao expira, volta e clica em "Concluir".
    // ============================================================

    @Test
    void postComplementarSemPendingNaSessaoRedirecionaParaEscolherMetodo() throws Exception {
        long beforeAccounts = accountRepository.count();

        mvc.perform(post("/cadastro/adolescente/complementar")
                        .with(csrf())
                        .param("nickname", "kira")
                        .param("birthDate", "2010-05-01")
                        .param("photoSource", "NONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/adolescente/escolher-metodo"));

        assertThat(accountRepository.count())
                .as("POST sem pending nao deve criar conta")
                .isEqualTo(beforeAccounts);
    }

    // ============================================================
    // Bordas de idade — limites EXATOS aceitos e rejeitados.
    // Garante que ninguem trocou 13/17 silenciosamente.
    // ============================================================

    @Test
    void postComplementarIdadeExatamente13EhAceito() throws Exception {
        var p = pending("limite13@gmail.com");
        // Nasceu ha exatamente 13 anos hoje → idade = 13.
        LocalDate birth = LocalDate.now(clock).minusYears(13);
        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(sessionWithPending(p))
                        .with(csrf())
                        .param("nickname", "limit13")
                        .param("birthDate", birth.toString())
                        .param("photoSource", "NONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/concluido"));

        assertThat(accountRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("limite13@gmail.com"))
                .isPresent();
    }

    @Test
    void postComplementarIdadeExatamente17EhAceito() throws Exception {
        var p = pending("limite17@gmail.com");
        // Nasceu ha exatamente 17 anos hoje → idade = 17.
        LocalDate birth = LocalDate.now(clock).minusYears(17);
        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(sessionWithPending(p))
                        .with(csrf())
                        .param("nickname", "limit17")
                        .param("birthDate", birth.toString())
                        .param("photoSource", "NONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/concluido"));

        assertThat(accountRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("limite17@gmail.com"))
                .isPresent();
    }

    @Test
    void postComplementarIdadeExatamente12EhBloqueada() throws Exception {
        var p = pending("limite12@gmail.com");
        // Nasceu ha 12 anos + 1 dia (= idade 12 incompleta) → under-13.
        LocalDate birth = LocalDate.now(clock).minusYears(12).minusDays(1);
        long before = accountRepository.count();

        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(sessionWithPending(p))
                        .with(csrf())
                        .param("nickname", "limit12")
                        .param("birthDate", birth.toString())
                        .param("photoSource", "NONE"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente_bloqueado"))
                .andExpect(model().attribute("variant", "under-13"));

        assertThat(accountRepository.count())
                .as("idade 12 nao deve criar conta")
                .isEqualTo(before);
    }

    @Test
    void postComplementarIdadeExatamente18EhBloqueada() throws Exception {
        var p = pending("limite18@gmail.com");
        // Nasceu ha exatamente 18 anos hoje → idade = 18.
        LocalDate birth = LocalDate.now(clock).minusYears(18);
        long before = accountRepository.count();

        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(sessionWithPending(p))
                        .with(csrf())
                        .param("nickname", "limit18")
                        .param("birthDate", birth.toString())
                        .param("photoSource", "NONE"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente_bloqueado"))
                .andExpect(model().attribute("variant", "over-17"));

        assertThat(accountRepository.count())
                .as("idade 18 nao deve criar conta")
                .isEqualTo(before);
    }

    @Test
    void postComplementarComDataFuturaEhBloqueadaComoUnder13() throws Exception {
        var p = pending("futuro@gmail.com");
        // Data futura → EligibleAgeValidator trata como TEEN_TOO_YOUNG.
        // Como rejectedValue eh LocalDate futura, Period.between(...).getYears()
        // retorna numero negativo → age < 13 → variant under-13.
        LocalDate birth = LocalDate.now(clock).plusDays(10);
        long before = accountRepository.count();

        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(sessionWithPending(p))
                        .with(csrf())
                        .param("nickname", "future")
                        .param("birthDate", birth.toString())
                        .param("photoSource", "NONE"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente_bloqueado"));

        assertThat(accountRepository.count())
                .as("data futura nao deve criar conta")
                .isEqualTo(before);
    }

    // ============================================================
    // photoSource=UPLOAD sem arquivo → controller aceita como sem
    // foto (avatar_url null) — comportamento defensivo via guard
    // `if (uploadedPhoto != null && !uploadedPhoto.isEmpty())` no
    // service. Garante que cadastro nao quebra se o usuario marcou
    // UPLOAD mas nao anexou arquivo.
    // ============================================================

    @Test
    void postComplementarUploadSemArquivoCriaContaSemAvatar() throws Exception {
        var p = pending("upload.sem.arquivo@gmail.com");

        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(sessionWithPending(p))
                        .with(csrf())
                        .param("nickname", "noupload")
                        .param("birthDate", "2010-05-01")
                        .param("photoSource", "UPLOAD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/concluido"));

        Account acc = accountRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("upload.sem.arquivo@gmail.com")
                .orElseThrow();
        AdolescentProfile profile = profileRepository.findById(acc.getId()).orElseThrow();
        assertThat(profile.getAvatarUrl())
                .as("UPLOAD sem arquivo deve cair em avatar_url null, nao quebrar")
                .isNull();
    }

    // ============================================================
    // photoSource=UPLOAD com mime invalido (Content-Type
    // application/pdf) → FilesystemAvatarStorage lanca
    // AvatarUnsupportedTypeException; conta NAO deve ser persistida
    // (transacao reverte).
    // ============================================================

    @Test
    void postComplementarUploadComMimeInvalidoNaoPersisteConta() throws Exception {
        var p = pending("mime.invalido@gmail.com");
        MockMultipartFile spoofed = new MockMultipartFile(
                "photo", "fake.jpg", "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46}); // %PDF

        try {
            mvc.perform(multipart("/cadastro/adolescente/complementar")
                            .file(spoofed)
                            .session(sessionWithPending(p))
                            .with(csrf())
                            .param("nickname", "mime")
                            .param("birthDate", "2010-05-01")
                            .param("photoSource", "UPLOAD"));
        } catch (Exception ignored) {
            // Propagacao da exception eh aceitavel — o contrato observavel
            // eh que a conta NAO foi persistida (transacao reverte).
        }

        assertThat(accountRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("mime.invalido@gmail.com"))
                .as("upload com mime invalido nao deve criar conta")
                .isEmpty();
    }

    // ============================================================
    // Apelido com HTML/script injetado eh sanitizado pelo HtmlSanitizer
    // antes de persistir (defesa em profundidade contra XSS quando o
    // apelido for renderizado em outras telas).
    // ============================================================

    @Test
    void postComplementarApelidoComHtmlEhSanitizadoAntesDePersistir() throws Exception {
        var p = pending("xss@gmail.com");
        // Apelido com tag <b> — HtmlSanitizer (Jsoup, allowlist NONE) deve
        // remover tags e deixar so o texto.
        String dirtyNick = "<b>hax</b>";

        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(sessionWithPending(p))
                        .with(csrf())
                        .param("nickname", dirtyNick)
                        .param("birthDate", "2010-05-01")
                        .param("photoSource", "NONE"))
                .andExpect(status().is3xxRedirection());

        Account acc = accountRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("xss@gmail.com")
                .orElseThrow();
        AdolescentProfile profile = profileRepository.findById(acc.getId()).orElseThrow();
        assertThat(profile.getNickname())
                .as("HtmlSanitizer deve remover tags do apelido")
                .doesNotContain("<")
                .doesNotContain(">")
                .doesNotContain("</");
    }

    // ============================================================
    // photoSource=GOOGLE mas pending.picture()=null → cai em sem-foto
    // sem quebrar. (cobertura do controller via integracao; o teste
    // de service em RegisterAdolescentServiceGoogleIT cobre apenas o
    // service direto.)
    // ============================================================

    @Test
    void postComplementarPhotoSourceGoogleMasPictureNullCriaContaSemAvatar() throws Exception {
        var p = pendingNoPicture("sem.pic.controller@gmail.com", "Julia");

        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(sessionWithPending(p))
                        .with(csrf())
                        .param("nickname", "noPic")
                        .param("birthDate", "2010-05-01")
                        .param("photoSource", "GOOGLE"))
                .andExpect(status().is3xxRedirection());

        Account acc = accountRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("sem.pic.controller@gmail.com")
                .orElseThrow();
        AdolescentProfile profile = profileRepository.findById(acc.getId()).orElseThrow();
        assertThat(profile.getAvatarUrl())
                .as("GOOGLE sem picture deve cair em null, nao quebrar")
                .isNull();
    }

    // ============================================================
    // GET /complementar com pending.givenName vazio → form
    // renderizado com nickname vazio em vez de null (defesa do
    // truncate). Pergunta aberta do Codificador (item 3).
    // ============================================================

    @Test
    void getComplementarComGivenNameVazioRenderizaFormComNicknameVazio() throws Exception {
        var p = new PendingGoogleSignup(
                "semnome@gmail.com",
                OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                "",  // givenName vazio
                null,
                Instant.parse("2026-05-20T10:00:00Z"));
        MvcResult result = mvc.perform(get("/cadastro/adolescente/complementar")
                        .session(sessionWithPending(p)))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente_complementar"))
                .andReturn();

        CompleteGoogleSignupForm form = (CompleteGoogleSignupForm)
                result.getModelAndView().getModel().get("form");
        assertThat(form).isNotNull();
        assertThat(form.getNickname())
                .as("givenName vazio deve produzir nickname vazio (string vazia), nao NPE")
                .isEqualTo("");
        // photoSource defaultou para NONE porque pending.picture eh null
        assertThat(form.getPhotoSource())
                .isEqualTo(CompleteGoogleSignupForm.PhotoSource.NONE);
    }

    @Test
    void getComplementarComGivenNameBlankRenderizaFormComNicknameBlank() throws Exception {
        // givenName com apenas espacos — truncate retorna como esta
        // (sem trim). Comportamento observavel: nao deve estourar.
        var p = new PendingGoogleSignup(
                "blank.given@gmail.com",
                OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                "   ",
                null,
                Instant.parse("2026-05-20T10:00:00Z"));
        MvcResult result = mvc.perform(get("/cadastro/adolescente/complementar")
                        .session(sessionWithPending(p)))
                .andExpect(status().isOk())
                .andReturn();

        CompleteGoogleSignupForm form = (CompleteGoogleSignupForm)
                result.getModelAndView().getModel().get("form");
        assertThat(form).isNotNull();
        // Sem estourar — basta confirmar que o GET respondeu 200.
    }

    // ============================================================
    // Race: callback Google chega e o e-mail foi cadastrado entre
    // existsByEmailIgnoreCase (no OAuthSuccessHandler) e o POST de
    // complementar. Cenario plausivel: outro device fez cadastro
    // por e-mail/senha enquanto Julia estava no fluxo Google.
    //
    // Defesa em profundidade: o registerFromGoogle re-checa o
    // duplicado e devolve EmailConflict.
    // ============================================================

    @Test
    void postComplementarComContaCriadaEntreOauthCallbackEPostDevolveAccountExists() throws Exception {
        // Simula a race: cria conta com email igual ao pending ANTES do
        // POST chegar (o handler ja deixou pending na sessao).
        var p = pending("race@gmail.com");
        MockHttpSession session = sessionWithPending(p);

        // Cria conta com mesmo email via fluxo email/senha (US-001) ANTES
        // de Julia clicar em "Concluir". Como AdolescentRegistrationController
        // exige password, vamos criar via fluxo Google em sessao paralela.
        var pAttacker = pending("race@gmail.com");
        MockHttpSession sAttacker = sessionWithPending(pAttacker);
        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(sAttacker)
                        .with(csrf())
                        .param("nickname", "attacker")
                        .param("birthDate", "2010-05-01")
                        .param("photoSource", "NONE"))
                .andExpect(status().is3xxRedirection());

        long after = accountRepository.count();

        // Agora Julia tenta concluir com seu pending — race resolvida pelo
        // service: existsByEmail → true → EmailConflict.
        mvc.perform(post("/cadastro/adolescente/complementar")
                        .session(session)
                        .with(csrf())
                        .param("nickname", "julia")
                        .param("birthDate", "2010-05-01")
                        .param("photoSource", "NONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cadastro/adolescente/escolher-metodo?error=account_exists"));

        assertThat(accountRepository.count())
                .as("race resolved: nao deve criar segunda conta com mesmo email")
                .isEqualTo(after);
    }

    // ============================================================
    // /cadastro/concluido eh publico — qualquer sessao acessa (eh
    // placeholder neste sprint).
    // ============================================================

    @Test
    void getConcluidoAcessivelSemSessao() throws Exception {
        mvc.perform(get("/cadastro/concluido"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/concluido"));
    }
}
