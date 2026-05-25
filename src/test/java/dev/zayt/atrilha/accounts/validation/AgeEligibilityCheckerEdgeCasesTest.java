package dev.zayt.atrilha.accounts.validation;

import dev.zayt.atrilha.accounts.domain.AccountRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge-case unit tests for {@link AgeEligibilityChecker} — gaps de borda
 * apontados pelo Codificador na entrega da Issue #36.
 *
 * <p>Cobre:</p>
 * <ul>
 *   <li>Ano bissexto: nascido em 29/02 em ano não-bissexto adjacente.</li>
 *   <li>Fusos: comportamento do verdict depende exclusivamente do
 *       {@link Clock} injetado (TZ-aware via {@link Clock#fixed} em zonas
 *       distintas no momento de virada de dia).</li>
 *   <li>Idades adultas extremas (centenárias) — sem limite superior para
 *       guardian (RF-E1-06).</li>
 *   <li>Aniversário exato em fevereiro 29 em ano bissexto (today == birth).</li>
 * </ul>
 *
 * <p>Não duplica os cenários já cobertos por
 * {@code AgeEligibilityCheckerTest}. Cada teste aqui exercita uma
 * regressão funcional plausível (mudança de {@code Period.between} para
 * cálculo manual, troca de {@code LocalDate.now()} direto, etc.).</p>
 */
class AgeEligibilityCheckerEdgeCasesTest {

    // ---------------------------------------------------------------
    //  Ano bissexto
    // ---------------------------------------------------------------

    /**
     * Nascido em 29/02/2008 (ano bissexto). "Hoje" em 28/02/2026 (não-bissexto).
     * Período exatamente "1 dia antes" do aniversário no ano não-bissexto;
     * a pessoa ainda não completou 18 anos — deve ser válida como TEEN.
     *
     * <p>Documenta o contrato: {@code Period.between} considera o aniversário
     * em ano não-bissexto como 01/03 (não 28/02). Se alguém implementar
     * "aniversário antecipado em ano não-bissexto", esse teste quebra.</p>
     */
    @Test
    void checkTeen_leapDayBirthInNonLeapYearDayBeforeBirthday_returnsEmpty() {
        Clock clock = fixedClockAt(LocalDate.of(2026, 2, 28));
        AgeEligibilityChecker checker = new AgeEligibilityChecker(clock);

        LocalDate birthDate = LocalDate.of(2008, 2, 29); // leap day
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.TEEN);

        // Em 28/02/2026 ainda tem 17 anos (Period.between conta 17y, 11m, 30d).
        assertThat(result).isEmpty();
    }

    /**
     * Mesmo nascimento 29/02/2008. "Hoje" em 01/03/2026 (não-bissexto).
     * Esse é o primeiro dia em que {@code Period.between} retorna 18 anos
     * — deve ser rejeitado como TEEN_TOO_OLD.
     */
    @Test
    void checkTeen_leapDayBirthInNonLeapYearDayAfterBirthday_returnsTeenTooOld() {
        Clock clock = fixedClockAt(LocalDate.of(2026, 3, 1));
        AgeEligibilityChecker checker = new AgeEligibilityChecker(clock);

        LocalDate birthDate = LocalDate.of(2008, 2, 29);
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.TEEN);

        assertThat(result).contains(AgeEligibilityViolation.TEEN_TOO_OLD);
    }

    /**
     * Mesmo nascimento 29/02/2008. "Hoje" em 29/02/2024 (próximo bissexto).
     * Aniversário exato em ano bissexto → 16 anos completos → TEEN válido.
     */
    @Test
    void checkTeen_leapDayBirthOnExactLeapBirthday_returnsEmpty() {
        Clock clock = fixedClockAt(LocalDate.of(2024, 2, 29));
        AgeEligibilityChecker checker = new AgeEligibilityChecker(clock);

        LocalDate birthDate = LocalDate.of(2008, 2, 29);
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.TEEN);

        assertThat(result).isEmpty();
    }

    /**
     * Nascido 29/02/2008 olhando do dia 13/02/2021 (não-bissexto).
     * Contagem deve ser 12 anos completos (aniversário em 2021 ainda não
     * ocorreu, e o de 2020 — bissexto — foi o último válido).
     * Resultado: TEEN_TOO_YOUNG (porque < 13).
     */
    @Test
    void checkTeen_leapDayBirthBeforeBirthdayIn13thYear_returnsTeenTooYoung() {
        Clock clock = fixedClockAt(LocalDate.of(2021, 2, 13));
        AgeEligibilityChecker checker = new AgeEligibilityChecker(clock);

        LocalDate birthDate = LocalDate.of(2008, 2, 29);
        Optional<AgeEligibilityViolation> result = checker.check(birthDate, AccountRole.TEEN);

        assertThat(result).contains(AgeEligibilityViolation.TEEN_TOO_YOUNG);
    }

    // ---------------------------------------------------------------
    //  Timezone — virada de dia depende exclusivamente do Clock
    // ---------------------------------------------------------------

    /**
     * Cenário: pessoa faria 13 anos em 20/05/2026 (00:00 hora local).
     * Momento absoluto avaliado: 20/05/2026 02:30 UTC == 19/05/2026 23:30
     * em America/Sao_Paulo (UTC-03).
     *
     * <p>Com Clock em SP: ainda é "ontem" → pessoa tem 12 anos → REJEITADO.<br>
     * Com Clock em UTC: já é o dia do aniversário → pessoa tem 13 anos → VÁLIDO.</p>
     *
     * <p>Esse teste garante que o veredito depende exclusivamente do
     * {@link Clock} injetado — e que a configuração de produção
     * ({@code America/Sao_Paulo} em {@link AgeEligibilityConfig}) materializa
     * a regra "hoje no fuso do produto".</p>
     */
    @Test
    void check_clockTimezoneControlsTodaysVerdictAtDayBoundary() {
        // Mesmo instante absoluto avaliado em duas zonas distintas.
        java.time.Instant atDayBoundary = LocalDateTime.of(2026, 5, 20, 2, 30)
                .atOffset(ZoneOffset.UTC)
                .toInstant();

        Clock clockSp = Clock.fixed(atDayBoundary, ZoneId.of("America/Sao_Paulo"));
        Clock clockUtc = Clock.fixed(atDayBoundary, ZoneOffset.UTC);

        LocalDate birthDate = LocalDate.of(2013, 5, 20); // 13 anos em 20/05/2026

        AgeEligibilityChecker spChecker = new AgeEligibilityChecker(clockSp);
        AgeEligibilityChecker utcChecker = new AgeEligibilityChecker(clockUtc);

        // Em SP ainda é 19/05/2026 23:30 → idade = 12 → TEEN_TOO_YOUNG.
        assertThat(spChecker.check(birthDate, AccountRole.TEEN))
                .contains(AgeEligibilityViolation.TEEN_TOO_YOUNG);

        // Em UTC já é 20/05/2026 02:30 → idade = 13 → válido.
        assertThat(utcChecker.check(birthDate, AccountRole.TEEN))
                .isEmpty();
    }

    /**
     * Cenário simétrico para a faixa GUARDIAN: virada de dia que decide se
     * a pessoa já fez 18.
     */
    @Test
    void checkGuardian_clockTimezoneControlsVerdictAtDayBoundary() {
        java.time.Instant atDayBoundary = LocalDateTime.of(2026, 5, 20, 2, 30)
                .atOffset(ZoneOffset.UTC)
                .toInstant();

        Clock clockSp = Clock.fixed(atDayBoundary, ZoneId.of("America/Sao_Paulo"));
        Clock clockUtc = Clock.fixed(atDayBoundary, ZoneOffset.UTC);

        LocalDate birthDate = LocalDate.of(2008, 5, 20); // 18 anos em 20/05/2026

        // Em SP ainda é 19/05 → 17 anos → GUARDIAN_TOO_YOUNG.
        assertThat(new AgeEligibilityChecker(clockSp).check(birthDate, AccountRole.GUARDIAN))
                .contains(AgeEligibilityViolation.GUARDIAN_TOO_YOUNG);

        // Em UTC já é 20/05 → 18 anos → válido.
        assertThat(new AgeEligibilityChecker(clockUtc).check(birthDate, AccountRole.GUARDIAN))
                .isEmpty();
    }

    // ---------------------------------------------------------------
    //  Idades extremas
    // ---------------------------------------------------------------

    /**
     * Guardian centenário (120 anos) — sem limite superior (RF-E1-06).
     * Se alguém adicionar limite superior por engano, esse teste quebra.
     */
    @Test
    void checkGuardian_centenarianAge_returnsEmpty() {
        Clock clock = fixedClockAt(LocalDate.of(2026, 5, 19));
        AgeEligibilityChecker checker = new AgeEligibilityChecker(clock);

        LocalDate birthDate = LocalDate.of(1906, 5, 19); // 120 anos hoje
        assertThat(checker.check(birthDate, AccountRole.GUARDIAN)).isEmpty();
    }

    /**
     * Teen idoso por erro (não deve existir, mas blindagem):
     * mesma data centenária recusada como TEEN (>= 18).
     */
    @Test
    void checkTeen_centenarianBirthDate_returnsTeenTooOld() {
        Clock clock = fixedClockAt(LocalDate.of(2026, 5, 19));
        AgeEligibilityChecker checker = new AgeEligibilityChecker(clock);

        LocalDate birthDate = LocalDate.of(1906, 5, 19);
        assertThat(checker.check(birthDate, AccountRole.TEEN))
                .contains(AgeEligibilityViolation.TEEN_TOO_OLD);
    }

    // ---------------------------------------------------------------
    //  Helpers
    // ---------------------------------------------------------------

    private static Clock fixedClockAt(LocalDate date) {
        return Clock.fixed(
                date.atStartOfDay(ZoneOffset.UTC).toInstant(),
                ZoneOffset.UTC);
    }
}
