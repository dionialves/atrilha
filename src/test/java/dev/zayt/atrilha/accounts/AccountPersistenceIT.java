package dev.zayt.atrilha.accounts;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração JPA + Flyway sobre a base real (Postgres 18 via Testcontainers).
 *
 * <p>Valida que:
 * <ul>
 *   <li>{@link Account} faz round-trip com todos os campos da migration V2.</li>
 *   <li>{@link AdolescentProfile} compartilha PK via {@code @MapsId}.</li>
 *   <li>{@link AccountRepository#findByEmailIgnoreCaseAndDeletedAtIsNull(String)} é
 *       case-insensitive e ignora soft-delete.</li>
 *   <li>{@link AccountRepository#existsByEmailIgnoreCaseAndDeletedAtIsNull(String)} idem.</li>
 * </ul>
 * </p>
 *
 * <p>Usa {@code @SpringBootTest} + {@code @DynamicPropertySource} apontando para
 * o Testcontainer Postgres — assim conseguimos contexto completo + JPA + Flyway
 * sem depender dos test slices opcionais do Spring Boot 4.x.</p>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                // IT roda em Postgres real com Flyway aplicado — sobrescreve o
                // perfil de teste padrão (H2 + Flyway off).
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@ActiveProfiles("test")
@DirtiesContext
@Transactional
class AccountPersistenceIT {

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
    AccountRepository accountRepository;

    @Autowired
    AdolescentProfileRepository profileRepository;

    @Autowired
    EntityManager entityManager;

    private Account newAdolescent(String email, String passwordHash) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setType("ADOLESCENT");
        a.setEmail(email);
        a.setPasswordHash(passwordHash);
        a.setCreatedAt(OffsetDateTime.now());
        return a;
    }

    @Test
    void accountAndProfileRoundTrip() {
        Account account = newAdolescent("rt@example.com", "$2b$12$" + "a".repeat(53));
        AdolescentProfile profile = new AdolescentProfile();
        profile.setAccount(account);
        profile.setNickname("julia");
        profile.setBirthDate(LocalDate.of(2010, 5, 1));
        profile.setTimezone("America/Sao_Paulo");

        accountRepository.saveAndFlush(account);
        profileRepository.saveAndFlush(profile);
        entityManager.flush();
        entityManager.clear();

        Account loaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(loaded.getEmail()).isEqualTo("rt@example.com");
        assertThat(loaded.getType()).isEqualTo("ADOLESCENT");
        assertThat(loaded.getPasswordHash()).startsWith("$2b$12$");
        assertThat(loaded.getEmailVerifiedAt()).isNull();
        assertThat(loaded.getDeletedAt()).isNull();

        AdolescentProfile loadedProfile = profileRepository.findById(account.getId()).orElseThrow();
        assertThat(loadedProfile.getNickname()).isEqualTo("julia");
        assertThat(loadedProfile.getBirthDate()).isEqualTo(LocalDate.of(2010, 5, 1));
        assertThat(loadedProfile.getTimezone()).isEqualTo("America/Sao_Paulo");
        assertThat(loadedProfile.getAvatarUrl()).isNull();
        // FK compartilhada via @MapsId: chave do profile = id da account
        assertThat(loadedProfile.getAccountId()).isEqualTo(account.getId());
    }

    @Test
    void findByEmailIgnoreCaseIsCaseInsensitive() {
        Account account = newAdolescent("Anna@Example.COM", "$2b$12$" + "b".repeat(53));
        accountRepository.saveAndFlush(account);
        entityManager.flush();
        entityManager.clear();

        Optional<Account> found = accountRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("anna@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(account.getId());
    }

    @Test
    void findByEmailIgnoreCaseSkipsSoftDeleted() {
        Account active = newAdolescent("active@example.com", "$2b$12$" + "c".repeat(53));
        Account deleted = newAdolescent("ghost@example.com", "$2b$12$" + "d".repeat(53));
        deleted.setDeletedAt(OffsetDateTime.now());
        accountRepository.saveAndFlush(active);
        accountRepository.saveAndFlush(deleted);
        entityManager.flush();
        entityManager.clear();

        assertThat(accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("active@example.com"))
                .isPresent();
        assertThat(accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("ghost@example.com"))
                .isEmpty();
    }

    @Test
    void existsByEmailIgnoreCaseAndDeletedAtIsNull() {
        Account account = newAdolescent("dup@example.com", "$2b$12$" + "e".repeat(53));
        accountRepository.saveAndFlush(account);
        entityManager.flush();
        entityManager.clear();

        assertThat(accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("DUP@example.com"))
                .isTrue();
        assertThat(accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("missing@example.com"))
                .isFalse();
    }
}
