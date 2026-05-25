package dev.zayt.atrilha.accounts.validation;

import dev.zayt.atrilha.accounts.domain.AccountRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link EligibleAge} + {@link EligibleAgeValidator}
 * — Bloco B da Issue #36.
 *
 * <p>Uses {@code @SpringBootTest} (caminho conservador descrito em Riscos #2
 * da Issue #36) para obter o {@code SpringConstraintValidatorFactory} que
 * resolve {@link AgeEligibilityChecker} via construtor. Um {@link Clock}
 * fixo é injetado como {@code @Primary} para tornar "hoje" determinístico.</p>
 */
@SpringBootTest(classes = EligibleAgeValidatorTest.TestApp.class)
@ActiveProfiles("test")
class EligibleAgeValidatorTest {

    static final LocalDate TODAY = LocalDate.of(2026, 5, 19);

    // Minimal Spring Boot context só com o necessário para validar
    // @EligibleAge — sem JPA, sem Mail. Evita pull do
    // EmailVerificationService (que vive no mesmo pacote e exige bean de
    // EmailVerificationTokenRepository, fora deste escopo).
    @org.springframework.boot.autoconfigure.SpringBootApplication(
            scanBasePackages = "dev.zayt.atrilha",
            exclude = {
                    org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration.class,
                    org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class,
                    org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration.class,
                    org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration.class
            })
    @org.springframework.context.annotation.ComponentScan(
            basePackages = {"dev.zayt.atrilha.auth", "dev.zayt.atrilha.accounts.validation"},
            useDefaultFilters = false,
            includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                    type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                    classes = AgeEligibilityChecker.class))
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

    // ---- Test target records anotados ----

    record TeenForm(@EligibleAge(role = AccountRole.TEEN) LocalDate birthDate) { }

    record GuardianForm(@EligibleAge(role = AccountRole.GUARDIAN) LocalDate birthDate) { }

    // ---- B1 ----
    @Test
    void validator_teenWithValidAge_noViolations() {
        TeenForm form = new TeenForm(TODAY.minusYears(15));
        Set<ConstraintViolation<TeenForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    // ---- B2 ----
    @Test
    void validator_teenWithAgeBelow13_oneViolationWithCorrectMessageKey() {
        TeenForm form = new TeenForm(TODAY.minusYears(10));
        Set<ConstraintViolation<TeenForm>> violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        ConstraintViolation<TeenForm> v = violations.iterator().next();
        assertThat(v.getMessageTemplate())
                .isEqualTo("{" + AgeEligibilityViolation.TEEN_TOO_YOUNG.messageKey() + "}");
    }

    // ---- B3 ----
    @Test
    void validator_teenWithAgeAbove17_oneViolationWithGuardianHintKey() {
        TeenForm form = new TeenForm(TODAY.minusYears(20));
        Set<ConstraintViolation<TeenForm>> violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        ConstraintViolation<TeenForm> v = violations.iterator().next();
        assertThat(v.getMessageTemplate())
                .isEqualTo("{" + AgeEligibilityViolation.TEEN_TOO_OLD.messageKey() + "}");
    }

    // ---- B4 ----
    @Test
    void validator_guardianWithAgeBelow18_oneViolationWithTeenHintKey() {
        GuardianForm form = new GuardianForm(TODAY.minusYears(17));
        Set<ConstraintViolation<GuardianForm>> violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        ConstraintViolation<GuardianForm> v = violations.iterator().next();
        assertThat(v.getMessageTemplate())
                .isEqualTo("{" + AgeEligibilityViolation.GUARDIAN_TOO_YOUNG.messageKey() + "}");
    }

    // ---- B5 ----
    @Test
    void validator_birthDateNull_noViolationFromEligibleAge() {
        TeenForm form = new TeenForm(null);
        Set<ConstraintViolation<TeenForm>> violations = validator.validate(form);
        // @EligibleAge in null = no violation (delegated to @NotNull elsewhere).
        assertThat(violations).isEmpty();
    }

    // ---- B6 ----
    @Test
    void validator_birthDateInFuture_violationWithTeenTooYoungKey() {
        // Decisão documentada no resumo de execução:
        // data futura é tratada como TEEN_TOO_YOUNG (idade negativa => < 13),
        // preservando o contrato observável "rejeitado".
        TeenForm form = new TeenForm(TODAY.plusDays(1));
        Set<ConstraintViolation<TeenForm>> violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        ConstraintViolation<TeenForm> v = violations.iterator().next();
        assertThat(v.getMessageTemplate())
                .isEqualTo("{" + AgeEligibilityViolation.TEEN_TOO_YOUNG.messageKey() + "}");
    }
}
