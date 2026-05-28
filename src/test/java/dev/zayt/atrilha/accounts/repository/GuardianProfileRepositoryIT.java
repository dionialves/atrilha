package dev.zayt.atrilha.accounts.repository;

import dev.zayt.atrilha.testsupport.AbstractSpringPostgresIT;

import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.domain.GuardianProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração JPA + Flyway sobre a base real (Postgres 18 via Testcontainers).
 *
 * <p>Valida que o {@link GuardianProfileRepository} faz round-trip com a
 * tabela criada pela migration V5, incluindo a relação 1:1 via {@code @MapsId}.</p>
 *
 * <p>Padrão idêntico ao {@code AccountPersistenceIT} — usa Testcontainer Postgres
 * com Flyway aplicado, não H2.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@ActiveProfiles("test")
@Transactional
class GuardianProfileRepositoryIT extends AbstractSpringPostgresIT {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    GuardianProfileRepository guardianProfileRepository;

    @Autowired
    EntityManager entityManager;

    private Account newGuardian(String email, String passwordHash) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setType("GUARDIAN");
        a.setEmail(email);
        a.setPasswordHash(passwordHash);
        a.setCreatedAt(OffsetDateTime.now());
        return a;
    }

    @Test
    void findByAccountId_whenExists() {
        Account account = newGuardian("carlos@example.com", "$2b$12$" + "x".repeat(53));
        GuardianProfile profile = new GuardianProfile();
        profile.setAccount(account);
        profile.setFullName("Carlos Responsável");

        accountRepository.saveAndFlush(account);
        guardianProfileRepository.saveAndFlush(profile);
        entityManager.flush();
        entityManager.clear();

        Optional<GuardianProfile> found = guardianProfileRepository.findByAccountId(account.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Carlos Responsável");
        assertThat(found.get().getAccountId()).isEqualTo(account.getId());
    }

    @Test
    void findByAccountId_whenNotFound() {
        UUID missingId = UUID.randomUUID();

        Optional<GuardianProfile> found = guardianProfileRepository.findByAccountId(missingId);

        assertThat(found).isEmpty();
    }
}
