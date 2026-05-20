package dev.zayt.atrilha.accounts;

import java.util.Optional;
import java.util.UUID;

/**
 * Leitura cross-module do apelido (nickname) associado a uma conta.
 *
 * <p>Exposta {@code public} para que módulos vizinhos (notably {@code auth}
 * durante a verificação de e-mail — US-006) saúdem o usuário pelo apelido
 * sem importar a entidade {@link AdolescentProfile} nem o repositório
 * package-private. Mantém a fronteira do módulo {@code accounts} respeitada
 * (PRD §9.3, {@code package-info.java}).</p>
 */
public interface AccountProfileLookup {

    /**
     * Retorna o apelido do perfil associado à conta dada, se houver.
     *
     * <p>Atualmente apenas {@link AdolescentProfile} é resolvido. Quando
     * {@code GuardianProfile} entrar (US-003), esta interface estende para
     * cobrir os dois tipos polimorficamente.</p>
     */
    Optional<String> findNickname(UUID accountId);
}
