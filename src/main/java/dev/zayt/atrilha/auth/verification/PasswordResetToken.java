package dev.zayt.atrilha.auth.verification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Token de recuperação de senha (US-008-a).
 *
 * <p>Persistido em {@code password_reset_token} (V6). Cada conta pode ter
 * vários tokens; apenas os com {@code usedAt IS NULL} e {@code expiresAt}
 * futuro são consideráveis. O service de recuperação invalida tokens
 * pendentes ao emitir um novo (resend).</p>
 *
 * <p>Visibilidade {@code public} para consumo pelo repositório e service.
 * A tabela é persistência do subdomínio "conta" — vive em {@code auth}
 * porque a orquestração (state machine de recuperação) é responsabilidade
 * do módulo {@code auth}.</p>
 *
 * <p>Deliberadamente sem {@code toString()} de Lombok — o valor do
 * {@code token} é segredo de uso único e NÃO deve aparecer em logs.
 * Caso seja necessário inspeção, usar {@code getId()}.</p>
 */
@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
public class PasswordResetToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "token", nullable = false, unique = true, updatable = false)
    private UUID token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
