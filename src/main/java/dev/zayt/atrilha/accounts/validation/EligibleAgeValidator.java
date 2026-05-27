package dev.zayt.atrilha.accounts.validation;

import dev.zayt.atrilha.accounts.domain.AccountRole;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

/**
 * {@link ConstraintValidator} para {@link EligibleAge}.
 *
 * <p>Delega a lógica de cálculo de idade a {@link AgeEligibilityChecker}
 * (injetado pelo {@code SpringConstraintValidatorFactory}) e devolve a
 * chave de mensagem correta conforme o tipo de violação.</p>
 *
 * <p>Convenção: {@code null} é tratado como válido (responsabilidade do
 * {@code @NotNull} de quem usa o campo). Data futura é interpretada como
 * idade negativa &Rightarrow; menor que a faixa permitida, gerando a
 * violação compatível com o {@link dev.zayt.atrilha.accounts.domain.AccountRole}
 * ({@code TEEN_TOO_YOUNG} ou {@code GUARDIAN_TOO_YOUNG}).</p>
 */
class EligibleAgeValidator
    implements ConstraintValidator<EligibleAge, LocalDate>
{

    private final AgeEligibilityChecker checker;
    private dev.zayt.atrilha.accounts.domain.AccountRole role;

    EligibleAgeValidator(AgeEligibilityChecker checker) {
        this.checker = checker;
    }

    @Override
    public void initialize(EligibleAge constraintAnnotation) {
        this.role = constraintAnnotation.role();
    }

    @Override
    public boolean isValid(
        LocalDate birthDate,
        ConstraintValidatorContext context
    ) {
        if (birthDate == null) {
            // delega a @NotNull
            return true;
        }
        try {
            var violation = checker.check(birthDate, role);
            if (violation.isEmpty()) {
                return true;
            }
            applyTemplate(context, violation.get().messageKey());
            return false;
        } catch (IllegalArgumentException e) {
            // data futura → trata como "muito jovem" para o role corrente,
            // preservando o contrato observável de "rejeitado".
            String key =
                role == AccountRole.TEEN
                    ? AgeEligibilityViolation.TEEN_TOO_YOUNG.messageKey()
                    : AgeEligibilityViolation.GUARDIAN_TOO_YOUNG.messageKey();
            applyTemplate(context, key);
            return false;
        }
    }

    private static void applyTemplate(
        ConstraintValidatorContext context,
        String key
    ) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate("{" + key + "}")
            .addConstraintViolation();
    }
}
