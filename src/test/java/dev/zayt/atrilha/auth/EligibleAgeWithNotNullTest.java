package dev.zayt.atrilha.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica a coexistência de {@code @NotNull} com {@link EligibleAge} num
 * mesmo campo (gap apontado pelo Codificador na entrega da Issue #36).
 *
 * <p>Contrato a manter:</p>
 * <ul>
 *   <li>Quando {@code birthDate == null}, somente {@code @NotNull} dispara.
 *       {@link EligibleAgeValidator#isValid} retorna {@code true} (delegação).</li>
 *   <li>Quando {@code birthDate} é não-nulo mas idade inválida, somente
 *       {@code @EligibleAge} dispara — {@code @NotNull} não emite ruído.</li>
 *   <li>Quando {@code birthDate} é não-nulo e idade válida, zero violações.</li>
 * </ul>
 *
 * <p>Esse cenário é especialmente relevante porque
 * {@code EligibleAgeValidator} hoje retorna {@code true} em {@code null} —
 * se alguém "consertar" esse comportamento para retornar {@code false}, a
 * mensagem do usuário ficaria errada (apareceria a key M1/M2/M3 em vez de
 * "campo obrigatório").</p>
 */
@SpringBootTest(classes = EligibleAgeWithNotNullTest.TestApp.class)
@ActiveProfiles("test")
class EligibleAgeWithNotNullTest {

    static final LocalDate TODAY = LocalDate.of(2026, 5, 19);

    @org.springframework.boot.autoconfigure.SpringBootApplication
    static class TestApp {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                    TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(),
                    ZoneOffset.UTC);
        }
    }

    @Autowired
    Validator validator;

    record TeenSignupForm(
            @NotNull
            @EligibleAge(role = AccountRole.TEEN)
            LocalDate birthDate) { }

    record GuardianSignupForm(
            @NotNull
            @EligibleAge(role = AccountRole.GUARDIAN)
            LocalDate birthDate) { }

    @Test
    void teenForm_nullBirthDate_onlyNotNullViolationFires() {
        TeenSignupForm form = new TeenSignupForm(null);
        Set<ConstraintViolation<TeenSignupForm>> violations = validator.validate(form);

        // Exatamente UMA violação — vinda de @NotNull, não de @EligibleAge.
        assertThat(violations).hasSize(1);

        ConstraintViolation<TeenSignupForm> v = violations.iterator().next();

        // O template não deve ser nenhuma das chaves de @EligibleAge.
        assertThat(v.getMessageTemplate())
                .doesNotContain(AgeEligibilityViolation.TEEN_TOO_YOUNG.messageKey())
                .doesNotContain(AgeEligibilityViolation.TEEN_TOO_OLD.messageKey())
                .doesNotContain(AgeEligibilityViolation.GUARDIAN_TOO_YOUNG.messageKey());

        // E deve corresponder à annotation @NotNull (descriptor).
        assertThat(v.getConstraintDescriptor().getAnnotation().annotationType())
                .isEqualTo(NotNull.class);
    }

    @Test
    void guardianForm_nullBirthDate_onlyNotNullViolationFires() {
        GuardianSignupForm form = new GuardianSignupForm(null);
        Set<ConstraintViolation<GuardianSignupForm>> violations = validator.validate(form);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getConstraintDescriptor().getAnnotation().annotationType())
                .isEqualTo(NotNull.class);
    }

    @Test
    void teenForm_validAge_zeroViolations() {
        TeenSignupForm form = new TeenSignupForm(TODAY.minusYears(15));
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void teenForm_ageTooYoung_onlyEligibleAgeViolationFires() {
        TeenSignupForm form = new TeenSignupForm(TODAY.minusYears(10));
        Set<ConstraintViolation<TeenSignupForm>> violations = validator.validate(form);

        assertThat(violations).hasSize(1);
        ConstraintViolation<TeenSignupForm> v = violations.iterator().next();
        assertThat(v.getMessageTemplate())
                .isEqualTo("{" + AgeEligibilityViolation.TEEN_TOO_YOUNG.messageKey() + "}");
        assertThat(v.getConstraintDescriptor().getAnnotation().annotationType())
                .isEqualTo(EligibleAge.class);
    }

    @Test
    void guardianForm_ageTooYoung_onlyEligibleAgeViolationFires() {
        GuardianSignupForm form = new GuardianSignupForm(TODAY.minusYears(17));
        Set<ConstraintViolation<GuardianSignupForm>> violations = validator.validate(form);

        assertThat(violations).hasSize(1);
        ConstraintViolation<GuardianSignupForm> v = violations.iterator().next();
        assertThat(v.getMessageTemplate())
                .isEqualTo("{" + AgeEligibilityViolation.GUARDIAN_TOO_YOUNG.messageKey() + "}");
        assertThat(v.getConstraintDescriptor().getAnnotation().annotationType())
                .isEqualTo(EligibleAge.class);
    }
}
