package dev.zayt.atrilha.auth.domain;

import dev.zayt.atrilha.accounts.domain.AccountRole;
import java.util.UUID;

/**
 * Principal de sessão simplificado — identifica a conta autenticada sem
 * acoplar a entidade JPA.
 *
 * <p>Carrega apenas {@code id} e {@code role}. Atributos adicionais (avatar,
 * apelido) ficam no perfil correspondente carregado por demanda.</p>
 */
public record AuthenticatedAccount(UUID id, AccountRole role) {
}
