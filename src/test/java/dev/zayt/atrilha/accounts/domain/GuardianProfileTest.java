package dev.zayt.atrilha.accounts.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários da entidade {@link GuardianProfile} (sem Spring, sem banco).
 *
 * <p>Valida que a entidade é instanciável e que os getters/setters de
 * {@code full_name} funcionam corretamente.</p>
 */
class GuardianProfileTest {

    @Test
    void full_nameNotNull() {
        GuardianProfile profile = new GuardianProfile();
        UUID accountId = UUID.randomUUID();

        profile.setAccountId(accountId);
        profile.setFullName("Maria Silva");

        assertThat(profile.getAccountId()).isEqualTo(accountId);
        assertThat(profile.getFullName()).isEqualTo("Maria Silva");
    }

    @Test
    void full_nameMaxLength() {
        GuardianProfile profile = new GuardianProfile();

        String fullName100 = "A".repeat(100);
        profile.setFullName(fullName100);

        assertThat(profile.getFullName()).hasSize(100);
        assertThat(profile.getFullName()).isEqualTo(fullName100);
    }
}
