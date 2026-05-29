package dev.zayt.atrilha.accounts.web;

import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.domain.AdolescentProfile;
import dev.zayt.atrilha.accounts.form.UpdateProfileForm;
import dev.zayt.atrilha.accounts.repository.AccountRepository;
import dev.zayt.atrilha.accounts.repository.AdolescentProfileRepository;
import dev.zayt.atrilha.accounts.service.UpdateProfileService;
import dev.zayt.atrilha.accounts.validation.AgeEligibilityViolation;
import dev.zayt.atrilha.auth.domain.AuthenticatedAccount;
import dev.zayt.atrilha.auth.domain.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/**
 * Controller de edição do perfil do adolescente (US-009).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>{@code GET /perfil} → carrega dados atuais do perfil e renderiza a view,
 *       populado com os dados atuais do perfil. O template (US-009-b) renderiza a tela de edição.</li>
 *   <li>{@code POST /perfil} → valida form, chama service, redireciona com flash message
 *       de sucesso ou re-renderiza com erros.</li>
 * </ul></p>
 */
@Controller
@RequestMapping("/perfil")
class ProfileController {

    private static final String EDIT_VIEW = "perfil/adolescente-editar";

    private final UpdateProfileService updateProfileService;
    private final AdolescentProfileRepository profileRepository;
    private final AccountRepository accountRepository;

    public ProfileController(UpdateProfileService updateProfileService,
                             AdolescentProfileRepository profileRepository,
                             AccountRepository accountRepository) {
        this.updateProfileService = updateProfileService;
        this.profileRepository = profileRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping
    String renderEdit(Authentication authentication, Model model) {
        UUID accountId = resolveAccountId(authentication);

        AdolescentProfile profile = profileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado"));

        // E-mail vem da tabela accounts (não de adolescent_profiles).
        String email = accountRepository.findById(accountId)
                .map(Account::getEmail)
                .orElse("");

        UpdateProfileForm form = new UpdateProfileForm();
        form.setNickname(profile.getNickname());
        form.setBirthDate(profile.getBirthDate());

        model.addAttribute("form", form);
        model.addAttribute("email", email);
        model.addAttribute("avatarUrl", profile.getAvatarUrl());
        return EDIT_VIEW;
    }

    @PostMapping
    String submit(@Valid @ModelAttribute("form") UpdateProfileForm form,
                  BindingResult bindingResult,
                  Authentication authentication,
                  Model model) {

        // Erros de Jakarta validation (ex.: nickname vazio) → re-renderiza.
        if (bindingResult.hasErrors()) {
            populateModelForReRender(authentication, model);
            return EDIT_VIEW;
        }

        UUID accountId = resolveAccountId(authentication);

        UpdateProfileService.Outcome outcome = updateProfileService.update(accountId, form);

        return switch (outcome) {
            case UpdateProfileService.Outcome.Updated ignored -> "redirect:/perfil?saved";
            case UpdateProfileService.Outcome.NicknameInvalid ignored -> {
                bindingResult.rejectValue("nickname", "profile.nickname.size",
                        "O apelido deve ter entre 3 e 20 caracteres.");
                populateModelForReRender(authentication, model);
                yield EDIT_VIEW;
            }
            case UpdateProfileService.Outcome.AgeViolation av -> {
                String code = switch (av.violation()) {
                    case TEEN_TOO_YOUNG -> "age.teen.tooYoung";
                    case TEEN_TOO_OLD -> "age.teen.tooOld";
                    default -> "age.invalid";
                };
                bindingResult.rejectValue("birthDate", code,
                        getAgeMessage(av.violation()));
                populateModelForReRender(authentication, model);
                yield EDIT_VIEW;
            }
        };
    }

    /**
     * Resolve o accountId a partir do SecurityContext. Suporta dois tipos de principal:
     * <ul>
     *   <li>{@link AuthenticatedAccount} — fluxo pós-cadastro (SessionAuthenticator).</li>
     *   <li>{@link AuthenticatedPrincipal} — fluxo de form login (AtrilhaUserDetails).</li>
     * </ul>
     */
    private UUID resolveAccountId(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedAccount acc) {
            return acc.id();
        }

        if (principal instanceof AuthenticatedPrincipal ap) {
            // AtrilhaUserDetails: o email é a chave. Busca no AccountRepository.
            String email = ap.getAccount().email();
            return accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                    .map(Account::getId)
                    .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada para: " + email));
        }

        throw new IllegalStateException("Principal desconhecido: " + principal.getClass());
    }

    /** Preenche o model com dados extras necessários para re-renderizar a view após erro. */
    private void populateModelForReRender(Authentication authentication, Model model) {
        UUID accountId = resolveAccountId(authentication);
        String email = accountRepository.findById(accountId)
                .map(Account::getEmail)
                .orElse("");
        AdolescentProfile profile = profileRepository.findByAccountId(accountId).orElse(null);

        model.addAttribute("email", email);
        model.addAttribute("avatarUrl", profile != null ? profile.getAvatarUrl() : null);
    }

    private String getAgeMessage(AgeEligibilityViolation violation) {
        return switch (violation) {
            case TEEN_TOO_YOUNG -> "Esse caminho ainda não é para você. Quando for a hora certa, a gente espera você por aqui.";
            case TEEN_TOO_OLD -> "Esse caminho é para 13 a 17 anos. Se você é responsável por uma adolescente, comece pelo caminho \"Sou responsável\" acima.";
            default -> "Verifique a data informada.";
        };
    }
}
