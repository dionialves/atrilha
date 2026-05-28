package dev.zayt.atrilha.auth.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(UUID token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.token = :token")
    Optional<PasswordResetToken> findByTokenForUpdate(@Param("token") UUID token);

    List<PasswordResetToken> findByAccountIdAndUsedAtIsNull(UUID accountId);

    void deleteByAccountIdAndUsedAtIsNull(UUID accountId);

    long countByAccountIdAndCreatedAtAfter(UUID accountId, Instant since);

    Optional<PasswordResetToken> findFirstByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
