package dev.zayt.atrilha.accounts.repository;

import dev.zayt.atrilha.testsupport.AbstractSpringPostgresIT;

import dev.zayt.atrilha.accounts.domain.AdolescentProfile;
import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.domain.GuardianProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para {@link JpaAccountProfileLookup} (US-003).
 *
 * <p>Valida que {@code findFullName()} resolve o full_name do GuardianProfile
 * e que {@code findDisplayName()} delega corretamente para full_name (GUARDIAN)
 * ou nickname (ADOLESCENT).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration"
        })
@ActiveProfiles("test")
@Transactional
class JpaAccountProfileLookupTest extends AbstractSpringPostgresIT {

    @Autowired
    private JpaAccountProfileLookup lookup;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private GuardianProfileRepository guardianProfileRepository;

    @Autowired
    private AdolescentProfileRepository adolescentProfileRepository;

    // ---- findFullName() ----

    @Test
    @DisplayName("findFullNameReturnsFullNameForGuardian")
    void findFullNameReturnsFullNameForGuardian() {
        UUID accountId = UUID.randomUUID();
        Account account = newAccount(accountId, "GUARDIAN", "maria-full-" + System.nanoTime() + "@teste.com");
        accountRepository.saveAndFlush(account);

        GuardianProfile profile = new GuardianProfile();
        profile.setAccount(account);
        profile.setFullName("Maria Silva");
        guardianProfileRepository.saveAndFlush(profile);

        Optional<String> result = lookup.findFullName(accountId);

        assertThat(result).isPresent().get().isEqualTo("Maria Silva");
    }

    @Test
    @DisplayName("findFullNameReturnsEmptyForAdolescentAccountId")
    void findFullNameReturnsEmptyForAdolescentAccountId() {
        UUID accountId = UUID.randomUUID();
        Account account = newAccount(accountId, "ADOLESCENT", "pedro-empty-" + System.nanoTime() + "@teste.com");
        accountRepository.saveAndFlush(account);

        Optional<String> result = lookup.findFullName(accountId);

        assertThat(result).isEmpty();
    }

    // ---- findDisplayName() ----

    @Test
    @DisplayName("findDisplayNameReturnsFullNameForGuardian")
    void findDisplayNameReturnsFullNameForGuardian() {
        UUID accountId = UUID.randomUUID();
        Account account = newAccount(accountId, "GUARDIAN", "ana-display-" + System.nanoTime() + "@teste.com");
        accountRepository.saveAndFlush(account);

        GuardianProfile profile = new GuardianProfile();
        profile.setAccount(account);
        profile.setFullName("Ana Silva");
        guardianProfileRepository.saveAndFlush(profile);

        Optional<String> result = lookup.findDisplayName(accountId, "GUARDIAN");

        assertThat(result).isPresent().get().isEqualTo("Ana Silva");
    }

    @Test
    @DisplayName("findDisplayNameReturnsNicknameForAdolescent")
    void findDisplayNameReturnsNicknameForAdolescent() {
        UUID accountId = UUID.randomUUID();
        Account account = newAccount(accountId, "ADOLESCENT", "pedro-nick-" + System.nanoTime() + "@teste.com");
        accountRepository.saveAndFlush(account);

        AdolescentProfile profile = new AdolescentProfile();
        profile.setAccount(account);
        profile.setNickname("Pedro");
        profile.setBirthDate(java.time.LocalDate.of(2013, 6, 15));
        profile.setTimezone("America/Sao_Paulo");
        adolescentProfileRepository.saveAndFlush(profile);

        Optional<String> result = lookup.findDisplayName(accountId, "ADOLESCENT");

        assertThat(result).isPresent().get().isEqualTo("Pedro");
    }

    // ---- Helpers ----

    private Account newAccount(UUID id, String type, String email) {
        Account a = new Account();
        a.setId(id);
        a.setType(type);
        a.setEmail(email);
        a.setPasswordHash("$2b$12$" + "x".repeat(53));
        a.setCreatedAt(OffsetDateTime.now());
        return a;
    }
}
