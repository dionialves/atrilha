package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.accounts.Account;
import dev.zayt.atrilha.accounts.AccountProfileLookup;
import dev.zayt.atrilha.auth.AccountRole;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * Implementacao JPA de {@link LoginAccountQuery} (FIX-013).
 *
 * <p>Consulta {@link dev.zayt.atrilha.accounts.AccountReader} pela tabela
 * {@code accounts} e devolve {@link LoginAccount} consumido pelo Spring Security
 * via {@link LoginAccountUserDetailsService}.</p>
 *
 * <p><b>Sprint 3 / vinculacao responsavel-adolescente:</b> a tabela de vinculo
 * ainda nao existe (chega em US-014). Por isso {@code hasGuardianLink}
 * retorna sempre {@code false} hoje. Quando US-014 entregar, atualizar este
 * metodo para consultar a tabela real e remover esta nota.</p>
 */
@Component
@ConditionalOnProperty(name = "atrilha.auth.seed.enabled", havingValue = "false", matchIfMissing = true)
class JpaLoginAccountQuery implements LoginAccountQuery {

    private final dev.zayt.atrilha.accounts.AccountReader accountReader;
    private final AccountProfileLookup adolescentProfileLookup;

    JpaLoginAccountQuery(dev.zayt.atrilha.accounts.AccountReader accountReader,
                         AccountProfileLookup adolescentProfileLookup) {
        this.accountReader = accountReader;
        this.adolescentProfileLookup = adolescentProfileLookup;
    }

    @Override
    public Optional<LoginAccount> findForLogin(String emailLowercase) {
        if (emailLowercase == null || emailLowercase.isBlank()) {
            return Optional.empty();
        }
        return accountReader.findByEmailIgnoreCase(emailLowercase)
                .map(this::toLoginAccount);
    }

    private LoginAccount toLoginAccount(Account account) {
        AccountRole role = "ADOLESCENT".equals(account.getType())
                ? AccountRole.TEEN
                : AccountRole.GUARDIAN;

        String displayName = resolveDisplayName(account, role);

        return new LoginAccount(
                account.getEmail().toLowerCase(Locale.ROOT),
                account.getPasswordHash(),     // pode ser null para contas OAuth-only (Google)
                role,
                false,                          // Sprint 3 — sem vinculo persistido ainda
                displayName);
    }

    private String resolveDisplayName(Account account, AccountRole role) {
        if (role == AccountRole.TEEN) {
            Optional<String> nickname = adolescentProfileLookup
                    .findNickname(account.getId());
            if (nickname.isPresent() && !nickname.get().isBlank()) {
                return nickname.get();
            }
        }
        // Fallback: parte antes do @ (consistente com InMemoryLoginAccountQuery)
        String email = account.getEmail();
        if (email == null || email.isBlank()) return "";
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
