package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração do orquestrador de cadastro de adolescente (US-001):
 * normaliza dados, faz hash da senha, sanitiza apelido, salva conta e perfil,
 * persiste foto opcional via {@link AvatarStorage}.
 *
 * <p>O bloqueio por idade ocorre na borda (via {@code @EligibleAge} no DTO);
 * o service assume input válido. Testes do controller cobrem o caminho de
 * bloqueio.</p>
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
class RegisterAdolescentServiceIT {

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
    PasswordEncoder passwordEncoder;

    private RegisterAdolescentRequest req(String email, String password, String nickname,
                                          LocalDate birthDate) {
        return new RegisterAdolescentRequest(email, password, nickname, birthDate);
    }

    @Test
    void happyPathPersistsAccountAndProfileWithBcryptPassword() {
        RegisterAdolescentRequest request = req(
                "julia@example.com", "supersecret1", "julia",
                LocalDate.of(2010, 5, 1));

        RegisterAdolescentService.Outcome outcome = service.register(request, null);

        assertThat(outcome).isInstanceOf(RegisterAdolescentService.Outcome.Registered.class);
        var registered = (RegisterAdolescentService.Outcome.Registered) outcome;

        Account account = accountRepository.findById(registered.accountId()).orElseThrow();
        assertThat(account.getEmail()).isEqualTo("julia@example.com");
        assertThat(account.getType()).isEqualTo("ADOLESCENT");
        assertThat(account.getPasswordHash())
                .as("BCrypt cost 12 prefix")
                .matches("^\\$2[aby]\\$12\\$.+");
        assertThat(passwordEncoder.matches("supersecret1", account.getPasswordHash()))
                .as("hash deve casar com a senha original")
                .isTrue();
        assertThat(account.getEmailVerifiedAt()).isNull();

        AdolescentProfile profile = profileRepository.findById(registered.accountId()).orElseThrow();
        assertThat(profile.getNickname()).isEqualTo("julia");
        assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(2010, 5, 1));
        assertThat(profile.getAvatarUrl()).isNull();
        assertThat(profile.getTimezone()).isEqualTo("America/Sao_Paulo");
    }

    @Test
    void emailIsStoredLowercaseAndTrimmed() {
        RegisterAdolescentRequest request = req(
                "  Julia2@Example.COM  ", "supersecret1", "julia2",
                LocalDate.of(2010, 5, 1));

        var outcome = (RegisterAdolescentService.Outcome.Registered) service.register(request, null);

        Account account = accountRepository.findById(outcome.accountId()).orElseThrow();
        assertThat(account.getEmail()).isEqualTo("julia2@example.com");
    }

    @Test
    void duplicateEmailReturnsConflictWithoutPersistingTwice() {
        RegisterAdolescentRequest first = req(
                "dup@example.com", "supersecret1", "alice",
                LocalDate.of(2010, 1, 1));
        service.register(first, null);

        long before = accountRepository.count();
        RegisterAdolescentRequest second = req(
                "DUP@example.com", "anothersecret", "bob",
                LocalDate.of(2009, 2, 2));

        RegisterAdolescentService.Outcome outcome = service.register(second, null);

        assertThat(outcome).isInstanceOf(RegisterAdolescentService.Outcome.EmailConflict.class);
        assertThat(accountRepository.count()).isEqualTo(before);
    }

    @Test
    void photoIsStoredViaAvatarStorageWhenPresent() {
        RegisterAdolescentRequest request = req(
                "withphoto@example.com", "supersecret1", "anna",
                LocalDate.of(2010, 5, 1));
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "anna.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});

        var outcome = (RegisterAdolescentService.Outcome.Registered) service.register(request, photo);

        AdolescentProfile profile = profileRepository.findById(outcome.accountId()).orElseThrow();
        assertThat(profile.getAvatarUrl())
                .isNotNull()
                .startsWith("/media/avatars/")
                .endsWith(".jpg")
                .contains(outcome.accountId().toString());
    }

    @Test
    void photoIsOptionalAndProfileAcceptsNullAvatar() {
        RegisterAdolescentRequest request = req(
                "nophoto@example.com", "supersecret1", "ben",
                LocalDate.of(2010, 5, 1));

        var outcome = (RegisterAdolescentService.Outcome.Registered) service.register(request, null);

        AdolescentProfile profile = profileRepository.findById(outcome.accountId()).orElseThrow();
        assertThat(profile.getAvatarUrl()).isNull();
    }

    @Test
    void emptyPhotoIsTreatedAsAbsent() {
        RegisterAdolescentRequest request = req(
                "emptyphoto@example.com", "supersecret1", "carla",
                LocalDate.of(2010, 5, 1));
        MockMultipartFile emptyPhoto = new MockMultipartFile(
                "photo", "ignored.jpg", "image/jpeg", new byte[0]);

        var outcome = (RegisterAdolescentService.Outcome.Registered) service.register(request, emptyPhoto);

        AdolescentProfile profile = profileRepository.findById(outcome.accountId()).orElseThrow();
        assertThat(profile.getAvatarUrl()).isNull();
    }

    @Test
    void nicknameIsSanitizedBeforePersistence() {
        RegisterAdolescentRequest request = req(
                "sanit@example.com", "supersecret1", "<b>julia</b>",
                LocalDate.of(2010, 5, 1));

        var outcome = (RegisterAdolescentService.Outcome.Registered) service.register(request, null);

        AdolescentProfile profile = profileRepository.findById(outcome.accountId()).orElseThrow();
        assertThat(profile.getNickname()).isEqualTo("julia");
    }
}
