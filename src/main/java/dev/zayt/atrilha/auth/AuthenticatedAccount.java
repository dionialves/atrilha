package dev.zayt.atrilha.auth;

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
