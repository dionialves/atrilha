package dev.zayt.atrilha.accounts.validation;

import dev.zayt.atrilha.accounts.domain.AccountRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AgeEligibilityChecker} — Bloco A da Issue #36.
 *
 * Tests are pure (no Spring context). A fixed Clock is injected to make
 * "today" deterministic across runs.
 */
class AgeEligibilityCheckerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 19);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(),
            ZoneOffset.UTC);

    private final AgeEligibilityChecker checker = new AgeEligibilityChecker(FIXED_CLOCK);

    // ----- TEEN -----

    @Test
    void checkTeen_birthDateMakingAgeBelow13_returnsTeenTooYoung() {
        LocalDate birthDate = LocalDate.of(2020, 1, 15); // 6 years old in 2026
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.TEEN);
        assertThat(result).contains(AgeEligibilityViolation.TEEN_TOO_YOUNG);
    }

    @Test
    void checkTeen_birthDateMakingAgeExactly13_returnsEmpty() {
        LocalDate birthDate = TODAY.minusYears(13); // exactly 13 today
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.TEEN);
        assertThat(result).isEmpty();
    }

    @Test
    void checkTeen_birthDateOneDayBefore13thBirthday_returnsTeenTooYoung() {
        LocalDate birthDate = TODAY.minusYears(13).plusDays(1); // 13 anniversary is tomorrow
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.TEEN);
        assertThat(result).contains(AgeEligibilityViolation.TEEN_TOO_YOUNG);
    }

    @Test
    void checkTeen_birthDateMakingAgeExactly17_returnsEmpty() {
        LocalDate birthDate = TODAY.minusYears(17);
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.TEEN);
        assertThat(result).isEmpty();
    }

    @Test
    void checkTeen_birthDateMakingAgeExactly18_returnsTeenTooOld() {
        LocalDate birthDate = TODAY.minusYears(18);
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.TEEN);
        assertThat(result).contains(AgeEligibilityViolation.TEEN_TOO_OLD);
    }

    @Test
    void checkTeen_birthDateOneDayBefore18thBirthday_returnsEmpty() {
        LocalDate birthDate = TODAY.minusYears(18).plusDays(1); // still 17
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.TEEN);
        assertThat(result).isEmpty();
    }

    // ----- GUARDIAN -----

    @Test
    void checkGuardian_birthDateMakingAgeBelow18_returnsGuardianTooYoung() {
        LocalDate birthDate = TODAY.minusYears(17);
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.GUARDIAN);
        assertThat(result).contains(AgeEligibilityViolation.GUARDIAN_TOO_YOUNG);
    }

    @Test
    void checkGuardian_birthDateMakingAgeExactly18_returnsEmpty() {
        LocalDate birthDate = TODAY.minusYears(18);
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.GUARDIAN);
        assertThat(result).isEmpty();
    }

    @Test
    void checkGuardian_birthDateOneDayBefore18thBirthday_returnsGuardianTooYoung() {
        LocalDate birthDate = TODAY.minusYears(18).plusDays(1);
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.GUARDIAN);
        assertThat(result).contains(AgeEligibilityViolation.GUARDIAN_TOO_YOUNG);
    }

    @Test
    void checkGuardian_veryOldAge_returnsEmpty() {
        LocalDate birthDate = TODAY.minusYears(90);
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.GUARDIAN);
        assertThat(result).isEmpty();
    }

    // ----- Input contract -----

    @Test
    void check_birthDateInFuture_throwsIllegalArgumentException() {
        LocalDate future = TODAY.plusDays(1);
        assertThatThrownBy(() -> checker.check(future, AccountRole.TEEN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void check_birthDateNull_throwsNullPointerException() {
        assertThatThrownBy(() -> checker.check(null, AccountRole.TEEN))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void check_roleNull_throwsNullPointerException() {
        LocalDate birthDate = TODAY.minusYears(15);
        assertThatThrownBy(() -> checker.check(birthDate, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void check_usesProvidedClock_notSystemClock() {
        // Same birth date, two different "todays" via different clocks → different verdicts.
        LocalDate birthDate = LocalDate.of(2010, 6, 1); // would be 15 on 2025-06-01 → valid TEEN

        Clock youngClock = Clock.fixed(
                LocalDate.of(2022, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant(),
                ZoneId.of("UTC")); // age 12 here → too young
        Clock oldClock = Clock.fixed(
                LocalDate.of(2030, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant(),
                ZoneId.of("UTC")); // age 20 here → too old

        AgeEligibilityChecker youngChecker = new AgeEligibilityChecker(youngClock);
        AgeEligibilityChecker oldChecker = new AgeEligibilityChecker(oldClock);

        assertThat(youngChecker.check(birthDate, AccountRole.TEEN))
                .contains(AgeEligibilityViolation.TEEN_TOO_YOUNG);
        assertThat(oldChecker.check(birthDate, AccountRole.TEEN))
                .contains(AgeEligibilityViolation.TEEN_TOO_OLD);
    }
}
