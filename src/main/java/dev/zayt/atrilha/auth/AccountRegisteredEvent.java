package dev.zayt.atrilha.auth;

import java.util.UUID;

/**
 * Evento de domínio publicado quando uma nova conta é cadastrada via
 * e-mail e senha (US-001).
 *
 * <p>Consumido pelo {@code AccountRegisteredEventListener} ({@code @TransactionalEventListener}
 * fase {@code AFTER_COMMIT}) para disparar o envio do e-mail de verificação
 * (US-006). A garantia AFTER_COMMIT é essencial: se a transação de cadastro
 * fizer rollback, nenhum e-mail é enviado, evitando que o usuário receba
 * link para uma conta que não existe.</p>
 *
 * <p>Pública porque o módulo {@code accounts} (publicador) e o módulo
 * {@code auth} (consumidor) vivem em pacotes diferentes — eventos são o
 * mecanismo canônico de comunicação entre módulos (PRD §9.3).</p>
 */
public record AccountRegisteredEvent(UUID accountId) {
}
