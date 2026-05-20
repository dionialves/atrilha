package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.auth.PendingGoogleSignup;
import dev.zayt.atrilha.notifications.RecordingEmailSender;
import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integracao do metodo {@code registerFromGoogle(...)} do
 * {@link RegisterAdolescentService} (US-002 — Issue #37).
 *
 * <p>Roda com Postgres real (Testcontainers) porque a US-001 exige
 * {@code ddl-auto=validate} + Flyway: nenhum teste de integracao roda em H2
 * para nao mascarar problemas de SQL Postgres-especificos (CHECK constraint
 * XOR, unique parcial por LOWER(email)).</p>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class RegisterAdolescentServiceGoogleIT {

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
    RegisterAdolescentService service;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AdolescentProfileRepository profileRepository;

    @Autowired
    RecordingEmailSender mailer;

    @Autowired
    PlatformTransactionManager txManager;

    @Autowired
    EntityManager em;

    TransactionTemplate tx;

    @BeforeEach
    void setup() {
        mailer.clear();
        tx = new TransactionTemplate(txManager);
    }

    private static PendingGoogleSignup pending(String email) {
        return new PendingGoogleSignup(
                email,
                OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                "Julia",
                "https://lh3.googleusercontent.com/a/julia",
                Instant.parse("2026-05-20T10:00:00Z"));
    }

    private static CompleteGoogleSignupRequest req(
            String nickname, LocalDate birthDate,
            CompleteGoogleSignupForm.PhotoSource photoSource) {
        return new CompleteGoogleSignupRequest(nickname, birthDate, photoSource);
    }

    // 7
    @Test
    void criaContaComOauthProviderGoogle() {
        var p = pending("julia.google@gmail.com");
        var r = req("julia", LocalDate.of(2010, 5, 1),
                CompleteGoogleSignupForm.PhotoSource.NONE);

        RegisterAdolescentService.Outcome outcome = service.registerFromGoogle(p, r, null);

        assertThat(outcome).isInstanceOf(RegisterAdolescentService.Outcome.GoogleRegistered.class);
        var success = (RegisterAdolescentService.Outcome.GoogleRegistered) outcome;
        Account account = accountRepository.findById(success.accountId()).orElseThrow();
        assertThat(account.getOauthProvider()).isEqualTo("google");
        assertThat(account.getPasswordHash()).isNull();
        assertThat(account.getEmailVerifiedAt()).isEqualTo(p.emailVerifiedAt());
        assertThat(account.getType()).isEqualTo("ADOLESCENT");

        AdolescentProfile profile = profileRepository.findById(success.accountId()).orElseThrow();
        assertThat(profile.getNickname()).isEqualTo("julia");
        assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(2010, 5, 1));
        assertThat(profile.getTimezone()).isEqualTo("America/Sao_Paulo");
    }

    // 8
    @Test
    void naoDisparaAccountRegisteredEvent() {
        var p = pending("noevent@gmail.com");
        var r = req("noevent", LocalDate.of(2010, 5, 1),
                CompleteGoogleSignupForm.PhotoSource.NONE);

        // Roda dentro de uma transacao explicita pra simular o ciclo de vida
        // real e dar chance ao AFTER_COMMIT do listener disparar. Se ele
        // disparasse, RecordingEmailSender registraria 1 e-mail.
        tx.executeWithoutResult(status -> service.registerFromGoogle(p, r, null));

        assertThat(mailer.recorded())
                .as("fluxo Google nao deve publicar AccountRegisteredEvent — e-mail Google ja vem verificado")
                .isEmpty();
    }

    // 9
    @Test
    void emailNormalizadoParaLowercase() {
        var p = new PendingGoogleSignup(
                "Joao@Gmail.com",
                OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                "Joao",
                null,
                Instant.parse("2026-05-20T10:00:00Z"));
        var r = req("joaoteen", LocalDate.of(2010, 5, 1),
                CompleteGoogleSignupForm.PhotoSource.NONE);

        var outcome = (RegisterAdolescentService.Outcome.GoogleRegistered)
                service.registerFromGoogle(p, r, null);

        Account account = accountRepository.findById(outcome.accountId()).orElseThrow();
        assertThat(account.getEmail()).isEqualTo("joao@gmail.com");
    }

    // 10
    @Test
    void conflitoDeEmailDevolveOutcomeEmailConflictSemPersistir() {
        // Cria primeiro a conta — fluxo Google.
        var p1 = pending("dup.google@gmail.com");
        var r1 = req("first", LocalDate.of(2010, 5, 1),
                CompleteGoogleSignupForm.PhotoSource.NONE);
        service.registerFromGoogle(p1, r1, null);

        long before = accountRepository.count();

        // Segunda chamada com mesmo e-mail (case diferente).
        var p2 = new PendingGoogleSignup(
                "DUP.GOOGLE@GMAIL.COM",
                OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                "Other", null,
                Instant.parse("2026-05-20T10:00:00Z"));
        var r2 = req("second", LocalDate.of(2010, 5, 1),
                CompleteGoogleSignupForm.PhotoSource.NONE);

        RegisterAdolescentService.Outcome outcome = service.registerFromGoogle(p2, r2, null);

        assertThat(outcome).isInstanceOf(RegisterAdolescentService.Outcome.EmailConflict.class);
        assertThat(accountRepository.count()).isEqualTo(before);
    }

    // 11
    @Test
    void avatarGoogleUrlPersistidoQuandoPhotoSourceGOOGLE() {
        var p = pending("foto.google@gmail.com");
        var r = req("fotogoog", LocalDate.of(2010, 5, 1),
                CompleteGoogleSignupForm.PhotoSource.GOOGLE);

        var outcome = (RegisterAdolescentService.Outcome.GoogleRegistered)
                service.registerFromGoogle(p, r, null);

        AdolescentProfile profile = profileRepository.findById(outcome.accountId()).orElseThrow();
        assertThat(profile.getAvatarUrl()).isEqualTo(p.picture());
        // E nao deve ter passado pelo AvatarStorage (verificacao indireta:
        // URL nao comeca com /media/avatars/).
        assertThat(profile.getAvatarUrl()).doesNotStartWith("/media/avatars/");
    }

    // 12
    @Test
    void avatarUploadPersistidoQuandoPhotoSourceUPLOAD() {
        var p = pending("foto.upload@gmail.com");
        var r = req("fotoup", LocalDate.of(2010, 5, 1),
                CompleteGoogleSignupForm.PhotoSource.UPLOAD);
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "anna.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});

        var outcome = (RegisterAdolescentService.Outcome.GoogleRegistered)
                service.registerFromGoogle(p, r, photo);

        AdolescentProfile profile = profileRepository.findById(outcome.accountId()).orElseThrow();
        assertThat(profile.getAvatarUrl())
                .isNotNull()
                .startsWith("/media/avatars/")
                .endsWith(".jpg")
                .contains(outcome.accountId().toString());
    }

    // 13
    @Test
    void semFotoAvatarUrlNulo() {
        var p = pending("nofoto.google@gmail.com");
        var r = req("nofoto", LocalDate.of(2010, 5, 1),
                CompleteGoogleSignupForm.PhotoSource.NONE);

        var outcome = (RegisterAdolescentService.Outcome.GoogleRegistered)
                service.registerFromGoogle(p, r, null);

        AdolescentProfile profile = profileRepository.findById(outcome.accountId()).orElseThrow();
        assertThat(profile.getAvatarUrl()).isNull();
    }

    // Extra: photoSource=GOOGLE mas pending.picture()=null → cai pra NONE
    @Test
    void photoSourceGoogleSemPictureFicaSemAvatar() {
        var p = new PendingGoogleSignup(
                "sempic@gmail.com",
                OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                "Sem", null,
                Instant.parse("2026-05-20T10:00:00Z"));
        var r = req("sempic", LocalDate.of(2010, 5, 1),
                CompleteGoogleSignupForm.PhotoSource.GOOGLE);

        var outcome = (RegisterAdolescentService.Outcome.GoogleRegistered)
                service.registerFromGoogle(p, r, null);

        AdolescentProfile profile = profileRepository.findById(outcome.accountId()).orElseThrow();
        assertThat(profile.getAvatarUrl()).isNull();
    }
}
