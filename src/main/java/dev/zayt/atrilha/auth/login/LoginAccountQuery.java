package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.accounts.domain.AccountRole;

import java.io.Serializable;
import java.util.Optional;

/**
 * Contrato SPI para consulta de conta de login (RF-E1-02).
 *
 * <p>As subtasks 007.01 a 007.04 implementam este contrato com JPA ou
 * stub em memória. Nenhuma classe ainda referencia {@code findForLogin} — a
 * implementação concreta vem na 007.03.</p>
 */
public interface LoginAccountQuery {

    /**
     * Busca a conta pelo e-mail (deve ser passado em minúsculas).
     *
     * @param emailLowercase e-mail normalizado para case-insensitive lookup
     * @return conta encontrada ou {@code Optional.empty()} se não existir
     */
    Optional<LoginAccount> findForLogin(String emailLowercase);

    /**
     * Registro imutável transportando os dados essenciais de uma conta.
     */
    record LoginAccount(
            String email,
            /* nullable para contas OAuth-only (sem senha local) */
            String passwordHashBcrypt,
            AccountRole role,
            boolean hasGuardianLink,
            String displayName) implements Serializable {
    }

}
