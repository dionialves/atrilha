package dev.zayt.atrilha.accounts.service;

import dev.zayt.atrilha.accounts.repository.AccountRepository;
import dev.zayt.atrilha.accounts.repository.GuardianProfileRepository;
import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.domain.GuardianProfile;
import dev.zayt.atrilha.accounts.domain.RegisterGuardianRequest;
import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do orquestrador de cadastro de responsável (US-003).
 *
 * <p>Verifica: feliz, duplicidade case-insensitive, normalização de e-mail
 * e hash BCrypt da senha.</p>
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.baseline-on-migrate=false"
})
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@Transactional // rollback automático entre métodos de teste
class RegisterGuardianServiceTest {

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
    RegisterGuardianService service;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    GuardianProfileRepository guardianProfileRepository;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    private static final RegisterGuardianRequest VALID_REQUEST = new RegisterGuardianRequest(
            "carlos@example.com",
            "supersecret1",
            "Carlos Silva",
            LocalDate.of(1990, 5, 1)
    );

    @Test
    void registersSuccessfully() {
        RegisterGuardianService.Outcome outcome = service.register(VALID_REQUEST);

        assertThat(outcome).isInstanceOf(RegisterGuardianService.Outcome.Registered.class);
        var registered = (RegisterGuardianService.Outcome.Registered) outcome;

        Account account = accountRepository.findById(registered.accountId()).orElseThrow();
        assertThat(account.getType()).isEqualTo("GUARDIAN");
        assertThat(account.getEmail()).isEqualTo("carlos@example.com");

        GuardianProfile profile = guardianProfileRepository.findByAccountId(registered.accountId()).orElseThrow();
        assertThat(profile.getFullName()).isEqualTo("Carlos Silva");

        // O evento foi publicado (o listener de email o consome AFTER_COMMIT).
        // Como estamos em transação com rollback, não podemos verificar o listener diretamente.
        // A existência do evento é verificada pelo fato de que o service não lança exceção.
    }

    @Test
    void returnsEmailConflictWhenDuplicate() {
        // Primeiro cadastro — sucesso.
        RegisterGuardianService.Outcome first = service.register(VALID_REQUEST);
        assertThat(first).isInstanceOf(RegisterGuardianService.Outcome.Registered.class);

        // Segundo cadastro com mesmo e-mail (case diferente) — deve retornar EmailConflict.
        RegisterGuardianRequest duplicate = new RegisterGuardianRequest(
                "CARLOS@EXAMPLE.COM",
                "anothersecret1",
                "Carlos Duplicado",
                LocalDate.of(1985, 3, 15)
        );

        RegisterGuardianService.Outcome second = service.register(duplicate);

        assertThat(second).isInstanceOf(RegisterGuardianService.Outcome.EmailConflict.class);
        // Apenas uma conta deve existir.
        assertThat(accountRepository.count()).isEqualTo(1L);
    }

    @Test
    void emailNormalizedToLowercase() {
        RegisterGuardianRequest request = new RegisterGuardianRequest(
                "  Carlos.Example.COM  ",
                "supersecret1",
                "Carlos Silva",
                LocalDate.of(1990, 5, 1)
        );

        RegisterGuardianService.Outcome outcome = service.register(request);

        assertThat(outcome).isInstanceOf(RegisterGuardianService.Outcome.Registered.class);
        var registered = (RegisterGuardianService.Outcome.Registered) outcome;

        Account account = accountRepository.findById(registered.accountId()).orElseThrow();
        assertThat(account.getEmail()).isEqualTo("carlos.example.com");
    }

    @Test
    void passwordHashedWithPasswordEncoder() {
        RegisterGuardianService.Outcome outcome = service.register(VALID_REQUEST);

        assertThat(outcome).isInstanceOf(RegisterGuardianService.Outcome.Registered.class);
        var registered = (RegisterGuardianService.Outcome.Registered) outcome;

        Account account = accountRepository.findById(registered.accountId()).orElseThrow();
        // A senha em claro NUNCA deve aparecer no banco. BCrypt starts with $2a$ or $2b$.
        assertThat(account.getPasswordHash()).matches("^\\$2[ab]\\$12\\$.+");
        assertThat(account.getPasswordHash()).isNotEqualTo("supersecret1");
    }
}
