package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.auth.AccountRole;
import dev.zayt.atrilha.auth.AuthenticatedAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Cenários adicionais de cadastro de adolescente (US-001) — bordas,
 * validação composta e contratos estruturais que ainda não estavam cobertos
 * pela suíte original do Codificador.
 *
 * <p>Foco em funcionalidade: cada teste, se falhar, indica que uma feature
 * (validação, gating de bloqueio, autenticação de sessão, contrato com
 * SecurityConfig) regrediu — não trata de texto, layout ou microcopy.</p>
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.baseline-on-migrate=false"
})
@ActiveProfiles("test")
@DirtiesContext
class AdolescentRegistrationEdgeCasesIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("atrilha")
                    .withUsername("atrilha")
                    .withPassword("atrilha");

    @DynamicPropertySource
    static void registerPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    WebApplicationContext ctx;

    @Autowired
    AccountRepository accountRepository;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    // ============================================================
    // Acesso à rota protegida sem sessão (SecurityConfig)
    // ============================================================

    /**
     * SecurityConfig declara {@code /verificar-email} como única rota
     * autenticada. Sem sessão, deve ser bloqueada por Spring Security — caso
     * o filter chain seja afrouxado por engano, esta verificação detecta.
     */
    @Test
    void getVerificarEmailWithoutSessionIsBlockedBySecurity() throws Exception {
        mvc.perform(get("/verificar-email"))
                .andExpect(status().is(not(200)));
    }

    // ============================================================
    // CSRF token inválido (não apenas ausente)
    // ============================================================

    @Test
    void postWithInvalidCsrfTokenReturns403() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .header("X-CSRF-TOKEN", "invalid-token-value")
                        .param("email", "csrf@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "kira")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // Validação composta — regra de gating de bloqueio por idade
    // ============================================================

    /**
     * Quando o usuário envia simultaneamente uma idade fora da faixa
     * (<13) e um e-mail malformado, o controller NÃO deve mostrar a tela
     * de bloqueio por idade — esse caminho é reservado para quando o único
     * problema é a faixa etária. Erro composto cai no form normal e os
     * outros campos válidos são preservados (CA-3/CA-4).
     */
    @Test
    void postUnderageWithInvalidEmailRendersFormNotAgeBlockTemplate() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "not-an-email")
                        .param("password", "supersecret1")
                        .param("nickname", "mixed")
                        .param("birthDate", LocalDate.now().minusYears(10).toString()))
                .andExpect(status().isOk())
                // Não pode ir para bloqueio quando há outros erros simultâneos
                .andExpect(view().name("cadastro/adolescente"))
                .andExpect(model().attributeHasFieldErrors("form", "email"))
                .andExpect(model().attributeHasFieldErrors("form", "birthDate"))
                // Apelido válido continua no form pra repopular
                .andExpect(content().string(containsString("mixed")));
    }

    /**
     * Idade fora + senha curta simultâneos: cai no form, não no bloqueio.
     */
    @Test
    void postUnderageWithShortPasswordRendersFormWithBothErrors() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "young@example.com")
                        .param("password", "abc12")
                        .param("nickname", "kept")
                        .param("birthDate", LocalDate.now().minusYears(10).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente"))
                .andExpect(model().attributeHasFieldErrors("form", "password"))
                .andExpect(model().attributeHasFieldErrors("form", "birthDate"))
                .andExpect(content().string(containsString("kept")));
    }

    // ============================================================
    // Limites de apelido
    // ============================================================

    @Test
    void postNicknameWithExactlyThreeCharsIsAccepted() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "min3@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "abc")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verificar-email"));
    }

    @Test
    void postNicknameWithExactlyTwentyCharsIsAccepted() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "max20@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "a".repeat(20))
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verificar-email"));
    }

    @Test
    void postNicknameWithTwentyOneCharsIsRejected() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "over20@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "a".repeat(21))
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente"))
                .andExpect(model().attributeHasFieldErrors("form", "nickname"));
    }

    @Test
    void postNicknameOnlyWhitespaceIsRejectedAsBlank() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "blank@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "   ")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente"))
                .andExpect(model().attributeHasFieldErrors("form", "nickname"));
    }

    // ============================================================
    // Limites de senha
    // ============================================================

    @Test
    void postPasswordWithExactlyEightCharsIsAccepted() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "pwd8@example.com")
                        .param("password", "12345678")
                        .param("nickname", "kira")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verificar-email"));
    }

    @Test
    void postPasswordWithSevenCharsIsRejected() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "pwd7@example.com")
                        .param("password", "1234567")
                        .param("nickname", "kira")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente"))
                .andExpect(model().attributeHasFieldErrors("form", "password"));
    }

    @Test
    void postPasswordWithSeventyTwoCharsIsAccepted() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "pwd72@example.com")
                        .param("password", "a".repeat(72))
                        .param("nickname", "kira")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verificar-email"));
    }

    @Test
    void postPasswordWithSeventyThreeCharsIsRejected() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "pwd73@example.com")
                        .param("password", "a".repeat(73))
                        .param("nickname", "kira")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro/adolescente"))
                .andExpect(model().attributeHasFieldErrors("form", "password"));
    }

    // ============================================================
    // BCrypt — cost 12 confirmado de ponta a ponta via controller
    // ============================================================

    /**
     * Confirma de ponta a ponta (controller → service → repo) que a senha
     * persistida usa BCrypt cost 12 — não apenas que o service faz isso
     * em isolamento.
     */
    @Test
    void registeredAccountHasBcryptCost12HashPersisted() throws Exception {
        mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "cost12@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "kira")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().is3xxRedirection());

        Account saved = accountRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("cost12@example.com")
                .orElseThrow();
        assertThat(saved.getPasswordHash())
                .as("BCrypt cost 12 prefix")
                .matches("^\\$2[aby]\\$12\\$.+");
        // BCrypt always 60 chars; serve de regressão contra trocar o encoder
        assertThat(saved.getPasswordHash()).hasSize(60);
    }

    // ============================================================
    // Sessão pós-cadastro — principal carrega o accountId correto
    // ============================================================

    /**
     * Após POST válido, a sessão armazenada via {@link HttpSessionSecurityContextRepository}
     * carrega um {@link AuthenticatedAccount} cujo id corresponde ao registro
     * recém-criado no banco. Garante que o contrato US-001 #5 — "fica logado
     * sem precisar logar de novo" — entrega o principal certo, não um
     * usuário anônimo nem um id aleatório.
     */
    @Test
    void postValidRegistrationStoresAuthenticatedAccountInSession() throws Exception {
        MvcResult result = mvc.perform(post("/cadastro/adolescente")
                        .with(csrf())
                        .param("email", "principal@example.com")
                        .param("password", "supersecret1")
                        .param("nickname", "principal")
                        .param("birthDate", "2010-05-01"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();

        SecurityContext securityContext = (SecurityContext)
                session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(securityContext).isNotNull();
        assertThat(securityContext.getAuthentication()).isNotNull();
        assertThat(securityContext.getAuthentication().isAuthenticated()).isTrue();

        Object principal = securityContext.getAuthentication().getPrincipal();
        assertThat(principal).isInstanceOf(AuthenticatedAccount.class);
        AuthenticatedAccount authed = (AuthenticatedAccount) principal;

        Account dbAccount = accountRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("principal@example.com")
                .orElseThrow();

        assertThat(authed.id()).isEqualTo(dbAccount.getId());
        assertThat(authed.role()).isEqualTo(AccountRole.TEEN);
    }

    // ============================================================
    // Foto — MIME falso (extensão JPG, Content-Type não-imagem)
    // ============================================================

    /**
     * Hoje o {@link FilesystemAvatarStorage} confia no header
     * {@code Content-Type}. Se o browser declarar {@code application/pdf}
     * (mesmo com nome de arquivo {@code .jpg}), o storage deve rejeitar.
     * Quando esse contrato regredir — porque alguém colocou validação
     * apenas pelo nome — este teste falha.
     */
    @Test
    void postWithSpoofedPdfDeclaredAsJpgExtensionRejectsUpload() throws Exception {
        MockMultipartFile spoofed = new MockMultipartFile(
                "photo", "selfie.jpg", "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46}); // %PDF magic bytes

        // O controller propaga AvatarUnsupportedTypeException — o resultado
        // observável é que NÃO há redirect 302 (cadastro não foi concluído)
        // e a conta não foi criada.
        try {
            mvc.perform(multipart("/cadastro/adolescente")
                            .file(spoofed)
                            .with(csrf())
                            .param("email", "spoofed@example.com")
                            .param("password", "supersecret1")
                            .param("nickname", "spoof")
                            .param("birthDate", "2010-05-01"));
        } catch (Exception ignored) {
            // Aceita propagação da exception — o contrato observável é que
            // o cadastro não foi persistido.
        }

        assertThat(accountRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("spoofed@example.com"))
                .as("spoofed PDF não deve resultar em conta criada")
                .isEmpty();
    }

    // ============================================================
    // Stub /cadastro/responsavel — contrato estrutural do template
    // ============================================================

    /**
     * O card "Sou responsável" da tela /comecar aponta para
     * /cadastro/responsavel. Esse link tem que resolver para 200 hoje
     * (mesmo sendo stub), senão o usuário vê 404 entre /comecar e o stub.
     * O teste original do controller já cobre o view name; aqui validamos
     * a integração: o template comecar.html contém um link para a rota
     * que efetivamente existe.
     */
    @Test
    void comecarHasLinkToCadastroResponsavelStubThatResolves() throws Exception {
        String comecarHtml = mvc.perform(get("/comecar"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(comecarHtml).contains("/cadastro/responsavel");

        // E a rota destino resolve hoje (stub).
        mvc.perform(get("/cadastro/responsavel"))
                .andExpect(status().isOk());
    }
}
