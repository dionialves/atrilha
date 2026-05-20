package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.auth.AccountRegisteredEvent;
import dev.zayt.atrilha.auth.PendingGoogleSignup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(RegisterAdolescentService.class);

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
     * Cadastra um adolescente a partir do callback OAuth do Google (US-002).
     *
     * <p>Diferencas em relacao a {@link #register}:
     * <ul>
     *   <li>{@code oauth_provider="google"} em vez de {@code password_hash}
     *       (XOR no banco — accounts_credential_chk).</li>
     *   <li>{@code email_verified_at} preenchido com o timestamp do
     *       {@code pending} — Google ja entregou e-mail verificado
     *       (RF-E1-07).</li>
     *   <li><strong>NAO</strong> publica {@link AccountRegisteredEvent} —
     *       isso dispararia a US-006 (e-mail de verificacao), que e
     *       redundante para contas Google.</li>
     *   <li>Avatar segue o {@link CompleteGoogleSignupForm.PhotoSource}:
     *       {@code GOOGLE} grava o URL da foto Google direto;
     *       {@code UPLOAD} delega ao {@link AvatarStorage};
     *       {@code NONE} deixa {@code avatar_url} null.</li>
     * </ul>
     * </p>
     */
    @Transactional
    Outcome registerFromGoogle(PendingGoogleSignup pending,
                               CompleteGoogleSignupRequest request,
                               MultipartFile uploadedPhoto) {
        String email = pending.email().trim().toLowerCase(Locale.ROOT);

        if (accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            return new Outcome.EmailConflict();
        }

        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setType("ADOLESCENT");
        account.setEmail(email);
        account.setOauthProvider("google");
        account.setEmailVerifiedAt(pending.emailVerifiedAt());
        account.setCreatedAt(OffsetDateTime.now());
        Account persistedAccount = accountRepository.saveAndFlush(account);

        AdolescentProfile profile = new AdolescentProfile();
        profile.setAccount(persistedAccount);
        profile.setNickname(htmlSanitizer.clean(request.nickname()));
        profile.setBirthDate(request.birthDate());
        profile.setTimezone("America/Sao_Paulo");

        switch (request.photoSource()) {
            case GOOGLE -> {
                String picture = pending.picture();
                if (picture != null && !picture.isBlank()) {
                    profile.setAvatarUrl(picture);
                }
            }
            case UPLOAD -> {
                if (uploadedPhoto != null && !uploadedPhoto.isEmpty()) {
                    profile.setAvatarUrl(avatarStorage.store(accountId, uploadedPhoto));
                }
            }
            case NONE -> {
                // avatar_url permanece null — fallback inicial do apelido.
            }
        }

        profileRepository.saveAndFlush(profile);

        // PRD §13.1: log estruturado sem PII. Instrumentacao real fica para US-069.
        log.info("account_created type=ADOLESCENT oauth_provider=google account_id={}", accountId);

        return new Outcome.GoogleRegistered(accountId);
    }

    /**
     * Resultado do cadastro. Sealed para forçar pattern matching exaustivo
     * no controller.
     */
    sealed interface Outcome permits Outcome.Registered, Outcome.GoogleRegistered, Outcome.EmailConflict {

        record Registered(UUID accountId) implements Outcome {
        }

        record GoogleRegistered(UUID accountId) implements Outcome {
        }

        record EmailConflict() implements Outcome {
        }
    }
}
