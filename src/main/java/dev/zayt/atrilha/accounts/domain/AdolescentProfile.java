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

import java.time.LocalDate;
import java.util.UUID;

/**
 * Perfil específico do adolescente (US-001 / PRD §10.1).
 *
 * <p>Relação 1:1 com {@link Account} via {@code @MapsId} — a PK do profile
 * é a mesma da account, garantindo cardinalidade exata no banco e
 * eliminando coluna `id` separada.</p>
 */
@Entity
@Table(name = "adolescent_profiles")
@Getter
@Setter
public class AdolescentProfile {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;
}
