package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.domain.AdolescentProfile;
import dev.zayt.atrilha.accounts.repository.AccountRepository;
import dev.zayt.atrilha.accounts.repository.AdolescentProfileRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Helper de teste para criar contas reais via JPA, substituindo o
 * stub {@code InMemoryLoginAccountQuery} que existia no Sprint 3.
 */
@Component
public class LoginTestFixtures {

    private final AccountRepository accountRepository;
    private final AdolescentProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginTestFixtures(AccountRepository accountRepository,
                             AdolescentProfileRepository profileRepository,
                             PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Account createTeenEmailPassword(String email, String rawPassword, String nickname) {
        UUID id = UUID.randomUUID();
        Account account = new Account();
        account.setId(id);
        account.setType("ADOLESCENT");
        account.setEmail(email.toLowerCase(Locale.ROOT));
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setEmailVerifiedAt(OffsetDateTime.now());
        account.setCreatedAt(OffsetDateTime.now());
        accountRepository.saveAndFlush(account);

        AdolescentProfile profile = new AdolescentProfile();
        profile.setAccount(account);
        profile.setNickname(nickname);
        profile.setBirthDate(LocalDate.now().minusYears(15));
        profile.setTimezone("America/Sao_Paulo");
        profileRepository.saveAndFlush(profile);

        return account;
    }

    public Account createGuardianEmailPassword(String email, String rawPassword) {
        UUID id = UUID.randomUUID();
        Account account = new Account();
        account.setId(id);
        account.setType("GUARDIAN");
        account.setEmail(email.toLowerCase(Locale.ROOT));
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setEmailVerifiedAt(OffsetDateTime.now());
        account.setCreatedAt(OffsetDateTime.now());
        accountRepository.saveAndFlush(account);
        return account;
    }
}
