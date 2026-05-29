package dev.zayt.atrilha.accounts.service;

import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.accounts.domain.AdolescentProfile;
import dev.zayt.atrilha.accounts.repository.AdolescentProfileRepository;
import dev.zayt.atrilha.accounts.validation.AgeEligibilityChecker;
import dev.zayt.atrilha.accounts.validation.AgeEligibilityViolation;
import dev.zayt.atrilha.accounts.form.UpdateProfileForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do {@link UpdateProfileService} (US-009-a).
 *
 * <p>Padrão: mock de dependências + AssertJ, compatível com Spring Boot 4.x.</p>
 */
class UpdateProfileServiceTest {

    private AdolescentProfileRepository profileRepo;
    private AgeEligibilityChecker ageChecker;
    private UpdateProfileService service;

    @BeforeEach
    void setUp() {
        profileRepo = mock(AdolescentProfileRepository.class);
        ageChecker = mock(AgeEligibilityChecker.class);
        service = new UpdateProfileService(profileRepo, ageChecker);
    }

    // ---------- Teste 1: happy path — nickname válido ----------

    @Test
    void shouldUpdateNicknameWhenValid() {
        UUID accountId = UUID.randomUUID();
        AdolescentProfile profile = createProfile(accountId, "Antigo", LocalDate.of(2010, 6, 15));
        when(profileRepo.findByAccountId(accountId)).thenReturn(Optional.of(profile));
        when(ageChecker.check(any(LocalDate.class), eq(AccountRole.TEEN))).thenReturn(Optional.empty());

        UpdateProfileForm form = new UpdateProfileForm();
        form.setNickname("Novo");
        form.setBirthDate(LocalDate.of(2010, 6, 15));

        var outcome = service.update(accountId, form);

        assertThat(outcome).isInstanceOf(UpdateProfileService.Outcome.Updated.class);
        assertThat(profile.getNickname()).isEqualTo("Novo");
        verify(profileRepo).saveAndFlush(profile);
    }

    // ---------- Teste 2: nickname muito curto (< 3) ----------

    @Test
    void shouldRejectNicknameTooShort() {
        UUID accountId = UUID.randomUUID();
        AdolescentProfile profile = createProfile(accountId, "Antigo", LocalDate.of(2010, 6, 15));
        when(profileRepo.findByAccountId(accountId)).thenReturn(Optional.of(profile));

        UpdateProfileForm form = new UpdateProfileForm();
        form.setNickname("Jo");
        form.setBirthDate(LocalDate.of(2010, 6, 15));

        var outcome = service.update(accountId, form);

        assertThat(outcome).isInstanceOf(UpdateProfileService.Outcome.NicknameInvalid.class);
        verify(profileRepo, never()).saveAndFlush(any());
    }

    // ---------- Teste 3: nickname muito longo (> 20) ----------

    @Test
    void shouldRejectNicknameTooLong() {
        UUID accountId = UUID.randomUUID();
        AdolescentProfile profile = createProfile(accountId, "Antigo", LocalDate.of(2010, 6, 15));
        when(profileRepo.findByAccountId(accountId)).thenReturn(Optional.of(profile));

        UpdateProfileForm form = new UpdateProfileForm();
        form.setNickname("abcdefghijklmnopqrstu"); // 21 chars
        form.setBirthDate(LocalDate.of(2010, 6, 15));

        var outcome = service.update(accountId, form);

        assertThat(outcome).isInstanceOf(UpdateProfileService.Outcome.NicknameInvalid.class);
        verify(profileRepo, never()).saveAndFlush(any());
    }

    // ---------- Teste 4: birthDate muito jovem (< 13) ----------

    @Test
    void shouldRejectBirthDateTooYoung() {
        UUID accountId = UUID.randomUUID();
        AdolescentProfile profile = createProfile(accountId, "Jovem", LocalDate.of(2015, 1, 1));
        when(profileRepo.findByAccountId(accountId)).thenReturn(Optional.of(profile));
        when(ageChecker.check(any(LocalDate.class), eq(AccountRole.TEEN)))
                .thenReturn(Optional.of(AgeEligibilityViolation.TEEN_TOO_YOUNG));

        UpdateProfileForm form = new UpdateProfileForm();
        form.setNickname("Jovem");
        form.setBirthDate(LocalDate.of(2015, 1, 1)); // ~10 anos

        var outcome = service.update(accountId, form);

        assertThat(outcome).isInstanceOfSatisfying(
                UpdateProfileService.Outcome.AgeViolation.class,
                av -> assertThat(av.violation()).isEqualTo(AgeEligibilityViolation.TEEN_TOO_YOUNG));
        verify(profileRepo, never()).saveAndFlush(any());
    }

    // ---------- Teste 5: birthDate muito velho (>= 18) ----------

    @Test
    void shouldRejectBirthDateTooOld() {
        UUID accountId = UUID.randomUUID();
        AdolescentProfile profile = createProfile(accountId, "Velho", LocalDate.of(2000, 1, 1));
        when(profileRepo.findByAccountId(accountId)).thenReturn(Optional.of(profile));
        when(ageChecker.check(any(LocalDate.class), eq(AccountRole.TEEN)))
                .thenReturn(Optional.of(AgeEligibilityViolation.TEEN_TOO_OLD));

        UpdateProfileForm form = new UpdateProfileForm();
        form.setNickname("Velho");
        form.setBirthDate(LocalDate.of(2000, 1, 1)); // ~25 anos

        var outcome = service.update(accountId, form);

        assertThat(outcome).isInstanceOfSatisfying(
                UpdateProfileService.Outcome.AgeViolation.class,
                av -> assertThat(av.violation()).isEqualTo(AgeEligibilityViolation.TEEN_TOO_OLD));
        verify(profileRepo, never()).saveAndFlush(any());
    }

    // ---------- Teste 6: avatarUrl e timezone não são alterados ----------

    @Test
    void shouldNotTouchProgressFieldsOnUpdate() {
        UUID accountId = UUID.randomUUID();
        AdolescentProfile profile = createProfile(accountId, "Antigo", LocalDate.of(2010, 6, 15));
        profile.setAvatarUrl("/media/avatars/" + accountId + ".jpg");
        profile.setTimezone("America/Sao_Paulo");
        when(profileRepo.findByAccountId(accountId)).thenReturn(Optional.of(profile));
        when(ageChecker.check(any(LocalDate.class), eq(AccountRole.TEEN))).thenReturn(Optional.empty());

        UpdateProfileForm form = new UpdateProfileForm();
        form.setNickname("Novo");
        form.setBirthDate(LocalDate.of(2010, 6, 15));

        service.update(accountId, form);

        assertThat(profile.getAvatarUrl()).isEqualTo("/media/avatars/" + accountId + ".jpg");
        assertThat(profile.getTimezone()).isEqualTo("America/Sao_Paulo");
        assertThat(profile.getNickname()).isEqualTo("Novo");
    }

    // ---------- Teste 7: perfil não encontrado lança IllegalArgumentException ----------

    @Test
    void shouldThrowWhenProfileNotFound() {
        UUID accountId = UUID.randomUUID();
        when(profileRepo.findByAccountId(accountId)).thenReturn(Optional.empty());

        UpdateProfileForm form = new UpdateProfileForm();
        form.setNickname("Novo");
        form.setBirthDate(LocalDate.of(2010, 6, 15));

        assertThatThrownBy(() -> service.update(accountId, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Perfil não encontrado");
    }

    // ---------- Teste 8: nickname vazio rejeitado ----------

    @Test
    void shouldRejectBlankNickname() {
        UUID accountId = UUID.randomUUID();
        AdolescentProfile profile = createProfile(accountId, "Antigo", LocalDate.of(2010, 6, 15));
        when(profileRepo.findByAccountId(accountId)).thenReturn(Optional.of(profile));

        UpdateProfileForm form = new UpdateProfileForm();
        form.setNickname("   ");
        form.setBirthDate(LocalDate.of(2010, 6, 15));

        var outcome = service.update(accountId, form);

        assertThat(outcome).isInstanceOf(UpdateProfileService.Outcome.NicknameInvalid.class);
        verify(profileRepo, never()).saveAndFlush(any());
    }

    // ---------- Helper ----------

    private AdolescentProfile createProfile(UUID accountId, String nickname, LocalDate birthDate) {
        AdolescentProfile p = new AdolescentProfile();
        p.setAccountId(accountId);
        p.setNickname(nickname);
        p.setBirthDate(birthDate);
        p.setTimezone("America/Sao_Paulo");
        return p;
    }
}
