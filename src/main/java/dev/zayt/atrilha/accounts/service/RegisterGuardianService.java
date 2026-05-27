package dev.zayt.atrilha.accounts.service;

import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.domain.GuardianProfile;
import dev.zayt.atrilha.accounts.domain.AccountRegisteredEvent;
import dev.zayt.atrilha.accounts.repository.AccountRepository;
import dev.zayt.atrilha.accounts.repository.GuardianProfileRepository;
import dev.zayt.atrilha.accounts.domain.RegisterGuardianRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Orquestrador do cadastro de responsável (US-003).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Normalizar e-mail para lower-case sem espaços de borda.</li>
 *   <li>Detectar duplicidade pré-persistência via repositório
 *       case-insensitive + soft-delete-aware.</li>
 *   <li>Fazer hash da senha (BCrypt cost 12).</li>
 *   <li>Persistir {@link Account} + {@link GuardianProfile}.</li>
 * </ul>
 * </p>
 *
 * <p>Não verifica idade — esse contrato vive na anotação
 * {@code @EligibleAge(role = GUARDIAN)} aplicada ao DTO, validada antes do
 * service ser chamado pelo controller. O service assume input válido por
 * Jakarta Validation.</p>
 *
 * <p>Resultado expresso pelo sealed {@link Outcome}:
 * {@link Outcome.Registered} (sucesso) ou {@link Outcome.EmailConflict}.</p>
 */
@Service
public class RegisterGuardianService {

    private final AccountRepository accountRepository;
    private final GuardianProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    RegisterGuardianService(AccountRepository accountRepository,
                            GuardianProfileRepository profileRepository,
                            PasswordEncoder passwordEncoder,
                            ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Outcome register(RegisterGuardianRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            return new Outcome.EmailConflict();
        }

        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setType("GUARDIAN");
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setCreatedAt(OffsetDateTime.now());
        // saveAndFlush — garante INSERT antes do FK do perfil cascatear.
        Account persistedAccount = accountRepository.saveAndFlush(account);

        GuardianProfile profile = new GuardianProfile();
        profile.setAccount(persistedAccount);
        profile.setFullName(request.fullName());

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
    public sealed interface Outcome permits Outcome.Registered, Outcome.EmailConflict {

        public record Registered(UUID accountId) implements Outcome {
        }

        public record EmailConflict() implements Outcome {
        }
    }
}
