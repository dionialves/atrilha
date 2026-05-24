package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.auth.login.LoginAccountQuery;

/**
 * Interface comum a {@link dev.zayt.atrilha.auth.login.AtrilhaUserDetails}
 * (form login) e {@link dev.zayt.atrilha.auth.login.AtrilhaOAuth2User}
 * (OAuth login), permitindo que controllers protegidos por papel leiam dados
 * do usuário sem se importar com a origem da autenticação.
 *
 * <p>Implementada por ambos os tipos de principal que o app injeta no
 * {@code SecurityContext} — form login e OAuth2 Google. O Spring Security
 * resolve automaticamente via {@code @AuthenticationPrincipal} quando o tipo
 * do parâmetro é esta interface.</p>
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
