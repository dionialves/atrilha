package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.auth.AccountRegisteredEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Orquestrador do cadastro de adolescente (US-001).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Normalizar e-mail para lower-case sem espaços de borda.</li>
 *   <li>Detectar duplicidade pré-persistência via repositório
 *       case-insensitive + soft-delete-aware.</li>
 *   <li>Sanitizar apelido (Jsoup) e fazer hash da senha (BCrypt cost 12).</li>
 *   <li>Persistir {@link Account} + {@link AdolescentProfile}.</li>
 *   <li>Opcionalmente persistir foto via {@link AvatarStorage}, atualizando
 *       {@code avatarUrl} no perfil.</li>
 * </ul>
 * </p>
 *
 * <p>Não verifica idade — esse contrato vive na anotação
 * {@code @EligibleAge(role = TEEN)} aplicada ao DTO, validada antes do
 * service ser chamado pelo controller. O service assume input válido por
 * Jakarta Validation.</p>
 *
 * <p>Resultado expresso pelo sealed {@link Outcome}:
 * {@link Outcome.Registered} (sucesso) ou {@link Outcome.EmailConflict}.</p>
 */
@Service
class RegisterAdolescentService {

    private final AccountRepository accountRepository;
    private final AdolescentProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorage avatarStorage;
    private final HtmlSanitizer htmlSanitizer;
    private final ApplicationEventPublisher eventPublisher;

    RegisterAdolescentService(AccountRepository accountRepository,
                              AdolescentProfileRepository profileRepository,
                              PasswordEncoder passwordEncoder,
                              AvatarStorage avatarStorage,
                              HtmlSanitizer htmlSanitizer,
                              ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarStorage = avatarStorage;
        this.htmlSanitizer = htmlSanitizer;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    Outcome register(RegisterAdolescentRequest request, MultipartFile photo) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            return new Outcome.EmailConflict();
        }

        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setType("ADOLESCENT");
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setCreatedAt(OffsetDateTime.now());
        // saveAndFlush — garante INSERT antes do FK do perfil cascatear.
        Account persistedAccount = accountRepository.saveAndFlush(account);

        AdolescentProfile profile = new AdolescentProfile();
        profile.setAccount(persistedAccount);
        profile.setNickname(htmlSanitizer.clean(request.nickname()));
        profile.setBirthDate(request.birthDate());
        profile.setTimezone("America/Sao_Paulo");

        if (photo != null && !photo.isEmpty()) {
            String url = avatarStorage.store(accountId, photo);
            profile.setAvatarUrl(url);
        }

        profileRepository.saveAndFlush(profile);

        // Dispara o fluxo de verificação de e-mail (US-006). O listener
        // escuta AFTER_COMMIT — se a transação cair, nenhum e-mail sai.
        eventPublisher.publishEvent(new AccountRegisteredEvent(accountId));

        return new Outcome.Registered(accountId);
    }

    /**
     * Resultado do cadastro. Sealed para forçar pattern matching exaustivo
     * no controller.
     */
    sealed interface Outcome permits Outcome.Registered, Outcome.EmailConflict {

        record Registered(UUID accountId) implements Outcome {
        }

        record EmailConflict() implements Outcome {
        }
    }
}
