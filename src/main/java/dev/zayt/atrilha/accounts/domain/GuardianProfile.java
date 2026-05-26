package dev.zayt.atrilha.accounts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Perfil específico do responsável (US-003 / PRD §10.1).
 *
 * <p>Relação 1:1 com {@link Account} via {@code @MapsId} — a PK do profile
 * é a mesma da account, garantindo cardinalidade exata no banco e
 * eliminando coluna {@code id} separada.</p>
 */
@Entity
@Table(name = "guardian_profiles")
@Getter
@Setter
public class GuardianProfile {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
}
