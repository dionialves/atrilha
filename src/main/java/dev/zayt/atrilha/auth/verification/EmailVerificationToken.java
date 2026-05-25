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
 * Token de verificação de e-mail (US-006).
 *
 * <p>Persistido em {@code email_verification_token} (V3). Cada conta pode ter
 * vários tokens; apenas os com {@code usedAt IS NULL} e {@code expiresAt}
 * futuro são consideráveis. O service de verificação invalida tokens
 * pendentes ao emitir um novo (resend).</p>
 *
 * <p>Visibilidade {@code public} para permitir referência por
 * {@code EmailVerificationService} (módulo {@code auth}). A
 * manipulação cross-module fica circunscrita ao service —
 * controllers/views consomem apenas {@code VerificationResult}.</p>
 */
@Entity
@Table(name = "email_verification_token")
@Getter
@Setter
public class EmailVerificationToken {

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

    /**
     * Deliberadamente sem {@code toString()} de Lombok — o valor do
     * {@code token} é segredo de uso único e NÃO deve aparecer em logs
     * (PRD §11.8). Caso seja necessário inspeção, usar {@code getId()}.
     */
}
