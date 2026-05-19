package dev.zayt.atrilha.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o contrato do record {@link AgeEligibilityResult}, alvo das US-002
 * e US-004 (caminho OAuth Google, onde a verificação não passa por Jakarta
 * Validation).
 *
 * <p>Contrato observável:</p>
 * <ul>
 *   <li>{@link AgeEligibilityResult#ok()} produz {@code eligible() == true}
 *       e {@code violation().isEmpty()}.</li>
 *   <li>{@link AgeEligibilityResult#denied(AgeEligibilityViolation)} produz
 *       {@code eligible() == false} e {@code violation()} contendo
 *       exatamente a violação informada.</li>
 *   <li>O record preserva a {@code violation} idêntica passada.</li>
 * </ul>
 */
class AgeEligibilityResultTest {

    @Test
    void ok_isEligibleAndEmpty() {
        AgeEligibilityResult result = AgeEligibilityResult.ok();
        assertThat(result.eligible()).isTrue();
        assertThat(result.violation()).isEmpty();
    }

    @Test
    void denied_teenTooYoung_isNotEligibleAndCarriesViolation() {
        AgeEligibilityResult result =
                AgeEligibilityResult.denied(AgeEligibilityViolation.TEEN_TOO_YOUNG);
        assertThat(result.eligible()).isFalse();
        assertThat(result.violation()).contains(AgeEligibilityViolation.TEEN_TOO_YOUNG);
    }

    @Test
    void denied_teenTooOld_carriesCorrectViolation() {
        AgeEligibilityResult result =
                AgeEligibilityResult.denied(AgeEligibilityViolation.TEEN_TOO_OLD);
        assertThat(result.eligible()).isFalse();
        assertThat(result.violation()).contains(AgeEligibilityViolation.TEEN_TOO_OLD);
    }

    @Test
    void denied_guardianTooYoung_carriesCorrectViolation() {
        AgeEligibilityResult result =
                AgeEligibilityResult.denied(AgeEligibilityViolation.GUARDIAN_TOO_YOUNG);
        assertThat(result.eligible()).isFalse();
        assertThat(result.violation()).contains(AgeEligibilityViolation.GUARDIAN_TOO_YOUNG);
    }

    @Test
    void factoryMethods_produceConsistentEquality() {
        // Dois "ok" devem ser iguais (record equality).
        assertThat(AgeEligibilityResult.ok()).isEqualTo(AgeEligibilityResult.ok());
        // Dois "denied" com mesma violação devem ser iguais.
        assertThat(AgeEligibilityResult.denied(AgeEligibilityViolation.TEEN_TOO_YOUNG))
                .isEqualTo(AgeEligibilityResult.denied(AgeEligibilityViolation.TEEN_TOO_YOUNG));
        // ok != denied
        assertThat(AgeEligibilityResult.ok())
                .isNotEqualTo(AgeEligibilityResult.denied(AgeEligibilityViolation.TEEN_TOO_YOUNG));
    }
}
