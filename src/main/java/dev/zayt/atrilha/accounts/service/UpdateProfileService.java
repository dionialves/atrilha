package dev.zayt.atrilha.accounts.service;

import dev.zayt.atrilha.accounts.avatar.AvatarStorage;
import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.accounts.domain.AdolescentProfile;
import dev.zayt.atrilha.accounts.repository.AdolescentProfileRepository;
import dev.zayt.atrilha.accounts.validation.AgeEligibilityChecker;
import dev.zayt.atrilha.accounts.validation.AgeEligibilityViolation;
import dev.zayt.atrilha.accounts.form.UpdateProfileForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Service de edição do perfil do adolescente (US-009).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Validar nickname (3–20 chars) e birthDate (idade 13–17 via AgeEligibilityChecker).</li>
 *   <li>Persistir alterações no AdolescentProfile sem tocar avatarUrl, timezone ou campos de progresso.</li>
 * </ul></p>
 *
 * <p>Resultado expresso pelo sealed {@link Outcome}:
 * {@link Outcome.Updated} (sucesso) ou {@link Outcome.NicknameInvalid} ou
 * {@link Outcome.AgeViolation}.</p>
 */
@Service
public class UpdateProfileService {

    private final AdolescentProfileRepository profileRepository;
    private final AgeEligibilityChecker ageEligibilityChecker;
    private final AvatarStorage avatarStorage;

    public UpdateProfileService(AdolescentProfileRepository profileRepository,
                                AgeEligibilityChecker ageEligibilityChecker,
                                AvatarStorage avatarStorage) {
        this.profileRepository = profileRepository;
        this.ageEligibilityChecker = ageEligibilityChecker;
        this.avatarStorage = avatarStorage;
    }

    @Transactional
    public Outcome update(UUID accountId, UpdateProfileForm form, MultipartFile photo, boolean removeAvatar) {
        AdolescentProfile profile = profileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado para a conta: " + accountId));

        // Validação de nickname (3–20 chars). Jakarta valida no controller, mas o service
        // faz double-check defensivo.
        String nickname = form.getNickname();
        if (nickname == null || nickname.isBlank() || nickname.length() < 3 || nickname.length() > 20) {
            return new Outcome.NicknameInvalid();
        }

        // Validação de idade via AgeEligibilityChecker
        var violation = ageEligibilityChecker.check(form.getBirthDate(), AccountRole.TEEN);
        if (violation.isPresent()) {
            return new Outcome.AgeViolation(violation.get());
        }

        // Aplica alterações — nickname e birthDate.
        profile.setNickname(nickname.trim());
        profile.setBirthDate(form.getBirthDate());

        // Foto: remove, salva nova ou mantém a existente.
        if (removeAvatar) {
            profile.setAvatarUrl(null);
        } else if (photo != null && !photo.isEmpty()) {
            String url = avatarStorage.store(accountId, photo);
            profile.setAvatarUrl(url);
        }

        profileRepository.saveAndFlush(profile);

        return new Outcome.Updated();
    }

    /**
     * Resultado da edição do perfil. Sealed para forçar pattern matching exaustivo
     * no controller.
     */
    public sealed interface Outcome permits Outcome.Updated, Outcome.NicknameInvalid, Outcome.AgeViolation {

        record Updated() implements Outcome {
        }

        record NicknameInvalid() implements Outcome {
        }

        record AgeViolation(AgeEligibilityViolation violation) implements Outcome {
        }
    }
}
