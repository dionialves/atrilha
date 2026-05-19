package dev.zayt.atrilha.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Conta polimórfica de usuário do atrilha (US-001 / PRD §10.1).
 *
 * <p>Coluna {@code type} discrimina entre {@code ADOLESCENT} e {@code GUARDIAN}.
 * Credencial é XOR entre {@code passwordHash} (cadastro por e-mail/senha,
 * US-001 / US-003) e {@code oauthProvider} (login social, US-002 / US-004).
 * Constraint de banco em {@code accounts_credential_chk}.</p>
 *
 * <p>Mantida {@code public} para uso entre módulos quando US-001 emitir
 * eventos de criação (necessário a partir da US-006 / US-012).</p>
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
public class Account {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Discriminator polimórfico: {@code ADOLESCENT} ou {@code GUARDIAN}. */
    @Column(name = "type", nullable = false, length = 16)
    private String type;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @Column(name = "password_hash", length = 72)
    private String passwordHash;

    @Column(name = "oauth_provider", length = 32)
    private String oauthProvider;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
