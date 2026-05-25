package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.accounts.domain.Account;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Factory de testes para {@link Account} — usada por testes em outros
 * pacotes (auth, notifications) que precisam de uma conta válida.
 *
 * <p>{@code public} apenas porque é consumida por testes em outros pacotes
 * de teste; não é exposta no jar principal.</p>
 */
public final class AccountTestFactory {

    private AccountTestFactory() {
        // utility — sem instâncias
    }

    public static Account newAdolescent(String email) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setType("ADOLESCENT");
        a.setEmail(email);
        a.setPasswordHash("$2b$12$" + "a".repeat(53));
        a.setCreatedAt(OffsetDateTime.now());
        return a;
    }
}
