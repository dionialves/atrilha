package dev.zayt.atrilha.accounts.validation;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.Optional;

/**
 * Service puro de elegibilidade por idade (US-005).
 *
 * <p>Recebe uma data de nascimento e um {@link dev.zayt.atrilha.accounts.domain.AccountRole};
 * devolve um {@link Optional} com a {@link AgeEligibilityViolation} aplicável,
 * ou vazio quando a faixa é válida.</p>
 *
 * <p>Faixas (ADR-006 / RF-E1-05/06):
 * <ul>
 *   <li>{@link dev.zayt.atrilha.accounts.domain.AccountRole#TEEN}: idade &ge; 13 e &lt; 18.</li>
 *   <li>{@link dev.zayt.atrilha.accounts.domain.AccountRole#GUARDIAN}: idade &ge; 18.</li>
 * </ul>
 * </p>
 *
 * <p>Sem persistência, sem logging de PII, sem dependência em
 * {@code LocalDate.now()} — o {@link Clock} injetado define "hoje".</p>
 */
@Component
class AgeEligibilityChecker {

    private final Clock clock;

    AgeEligibilityChecker(Clock clock) {
        this.clock = clock;
    }

    Optional<AgeEligibilityViolation> check(LocalDate birthDate, dev.zayt.atrilha.accounts.domain.AccountRole role) {
        Objects.requireNonNull(birthDate, "birthDate");
        Objects.requireNonNull(role, "role");

        LocalDate today = LocalDate.now(clock);
        if (birthDate.isAfter(today)) {
            throw new IllegalArgumentException("birthDate in the future");
        }

        int age = Period.between(birthDate, today).getYears();
        return switch (role) {
            case TEEN -> {
                if (age < 13) {
                    yield Optional.of(AgeEligibilityViolation.TEEN_TOO_YOUNG);
                }
                if (age >= 18) {
                    yield Optional.of(AgeEligibilityViolation.TEEN_TOO_OLD);
                }
                yield Optional.empty();
            }
            case GUARDIAN -> age < 18
                    ? Optional.of(AgeEligibilityViolation.GUARDIAN_TOO_YOUNG)
                    : Optional.empty();
        };
    }
}
