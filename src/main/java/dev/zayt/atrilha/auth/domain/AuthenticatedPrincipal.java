package dev.zayt.atrilha.auth.domain;

import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.auth.login.LoginAccountQuery;

/**
 * Interface comum aos principals de form login (atualmente
 * {@link dev.zayt.atrilha.auth.login.AtrilhaUserDetails}), permitindo que
 * controllers protegidos por papel leiam dados do usuário sem se importar
 * com a origem da autenticação.
 *
 * <p>O Spring Security resolve automaticamente via
 * {@code @AuthenticationPrincipal} quando o tipo do parâmetro é esta
 * interface.</p>
 */
public interface AuthenticatedPrincipal {

    /** Retorna o papel da conta (TEEN ou GUARDIAN). */
    AccountRole role();

    /** Retorna o nome de exibição do usuário. */
    String displayName();

    /** Retorna true se o responsável tem ao menos um filho vinculado. */
    boolean hasGuardianLink();

    /** Retorna a conta original de login. */
    LoginAccountQuery.LoginAccount getAccount();
}
