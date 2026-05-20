package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.accounts.Account;
import dev.zayt.atrilha.accounts.AccountProfileLookup;
import dev.zayt.atrilha.accounts.AccountReader;
import dev.zayt.atrilha.notifications.EmailVerificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;
import java.util.UUID;

/**
 * Reage a {@link AccountRegisteredEvent} disparando a verificação de e-mail
 * (US-006 §M / Issue #39).
 *
 * <p>Importante: usa {@link TransactionPhase#AFTER_COMMIT}. Isso garante que
 * o e-mail só é enviado se o cadastro persistiu de fato — qualquer rollback
 * no upstream impede o envio, prevenindo links órfãos.</p>
 *
 * <p>Visibilidade package-private; o registro ocorre via Spring DI a partir
 * do {@code @Component}.</p>
 */
@Component
class AccountRegisteredEventListener {

    private static final Logger log = LoggerFactory.getLogger(AccountRegisteredEventListener.class);

    private final EmailVerificationService verificationService;
    private final EmailVerificationSender sender;
    private final AccountReader accountReader;
    private final AccountProfileLookup profileLookup;

    AccountRegisteredEventListener(EmailVerificationService verificationService,
                                   EmailVerificationSender sender,
                                   AccountReader accountReader,
                                   AccountProfileLookup profileLookup) {
        this.verificationService = verificationService;
        this.sender = sender;
        this.accountReader = accountReader;
        this.profileLookup = profileLookup;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onAccountRegistered(AccountRegisteredEvent event) {
        // Propagation.REQUIRES_NEW: o listener roda DEPOIS do commit principal,
        // num escopo onde a TransactionSynchronizationManager ainda tem o
        // estado "transação concluída" pendurado. Forçar uma nova garante que
        // o JPA consiga reabrir o contexto de persistência para o INSERT do token.
        UUID accountId = event.accountId();
        Optional<Account> accountOpt = accountReader.findById(accountId);
        if (accountOpt.isEmpty()) {
            log.warn("account-registered event for unknown account; skipping send");
            return;
        }
        Account account = accountOpt.get();
        if (account.getEmailVerifiedAt() != null) {
            // Conta já chegou verificada (caso futuro Google OAuth — US-002) —
            // não dispara fluxo de verificação.
            return;
        }

        UUID token = verificationService.issueToken(account);
        String nickname = profileLookup.findNickname(accountId).orElse("");
        try {
            sender.sendVerification(account.getEmail(), nickname, token);
        } catch (RuntimeException e) {
            // Falha de SMTP não pode bombardear o pós-commit (a conta já foi
            // criada; o token já está no banco). O usuário consegue pedir
            // reenvio na própria tela /verificar-email. Log sem token nem
            // corpo (PRD §11.8) — apenas o tipo do erro e o destinatário.
            log.warn("verification email dispatch failed to={} cause={}",
                    account.getEmail(), e.getClass().getSimpleName());
        }
    }
}
