package dev.zayt.atrilha.auth;

import java.util.Optional;

/**
 * Resultado da verificação de elegibilidade por idade.
 *
 * <p>Reservado para chamadores que não passam por Jakarta Validation
 * (ex.: callback OAuth Google em US-002/US-004).</p>
 */
public record AgeEligibilityResult(Optional<AgeEligibilityViolation> violation) {

    public static AgeEligibilityResult ok() {
        return new AgeEligibilityResult(Optional.empty());
    }

    public static AgeEligibilityResult denied(AgeEligibilityViolation violation) {
        return new AgeEligibilityResult(Optional.of(violation));
    }

    public boolean eligible() {
        return violation.isEmpty();
    }
}
