package dev.zayt.atrilha.auth;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restrição Jakarta Validation que rejeita uma {@link java.time.LocalDate}
 * cuja idade caia fora da faixa esperada para o {@link AccountRole}
 * informado (US-005).
 *
 * <p>Faixas (ADR-006 / RF-E1-05/06):
 * <ul>
 *   <li>{@link AccountRole#TEEN}: idade &ge; 13 e &lt; 18.</li>
 *   <li>{@link AccountRole#GUARDIAN}: idade &ge; 18.</li>
 * </ul>
 * </p>
 *
 * <p>O texto da violação é resolvido por chave em {@code messages.properties}
 * — a chave concreta depende de qual tipo de inelegibilidade ocorreu
 * (ver {@link AgeEligibilityViolation}).</p>
 *
 * <p>Valor {@code null} é considerado válido por esta anotação — combine
 * com {@code @NotNull} quando o campo for obrigatório.</p>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EligibleAgeValidator.class)
public @interface EligibleAge {

    AccountRole role();

    String message() default "{validation.age.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
