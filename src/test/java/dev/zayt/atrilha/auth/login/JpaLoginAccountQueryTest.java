package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.testsupport.AbstractSpringPostgresIT;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para {@link JpaLoginAccountQuery} focados em GUARDIAN
 * (US-003).
 *
 * <p>Valida que {@code resolveDisplayName()} retorna o full_name do
 * GuardianProfile quando a conta é GUARDIAN, e faz fallback para prefixo
 * do email quando não há perfil.</p>
 */
@SpringBootTest(classes = { JpaLoginAccountQueryTest.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "atrilha.auth.seed.enabled=false"
        })
@ActiveProfiles("test")
class JpaLoginAccountQueryTest extends AbstractSpringPostgresIT {

    @Autowired
    private LoginAccountQuery loginAccountQuery;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---- resolveDisplayName() para GUARDIAN ----

    @Test
    @DisplayName("resolveDisplayNameUsesFullNameForGuardian")
    void resolveDisplayNameUsesFullNameForGuardian() {
        UUID accountId = UUID.randomUUID();
        String email = "carlos@teste.com";
        String hashed = passwordEncoder.encode("senha");

        insertAccount(accountId, "GUARDIAN", email, hashed);
        insertGuardianProfile(accountId, "Carlos Responsável");

        Optional<LoginAccountQuery.LoginAccount> result = loginAccountQuery.findForLogin(email);

        assertThat(result).isPresent();
        assertThat(result.get().displayName()).isEqualTo("Carlos Responsável");
    }

    @Test
    @DisplayName("resolveDisplayNameFallsBackToEmailPrefixForGuardianWhenNoProfile")
    void resolveDisplayNameFallsBackToEmailPrefixForGuardianWhenNoProfile() {
        UUID accountId = UUID.randomUUID();
        String email = "guardian@teste.com";

        insertAccount(accountId, "GUARDIAN", email, passwordEncoder.encode("senha"));
        // Não cria GuardianProfile — cenário de borda.

        Optional<LoginAccountQuery.LoginAccount> result = loginAccountQuery.findForLogin(email);

        assertThat(result).isPresent();
        assertThat(result.get().displayName()).isEqualTo("guardian");
    }

    // ---- Helpers ----

    private void insertAccount(UUID id, String type, String email, String passwordHash) {
        jdbcTemplate.update(
                """
                        INSERT INTO accounts (id, type, email, password_hash, created_at)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                id, type, email, passwordHash, OffsetDateTime.now());
    }

    private void insertGuardianProfile(UUID accountId, String fullName) {
        jdbcTemplate.update(
                """
                        INSERT INTO guardian_profiles (account_id, full_name)
                        VALUES (?, ?)
                        """,
                accountId, fullName);
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestBeans {
        // InMemoryLoginAccountQuery desligado por atrilha.auth.seed.enabled=false
    }
}
