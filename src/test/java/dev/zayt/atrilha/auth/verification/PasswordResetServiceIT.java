package dev.zayt.atrilha.auth.verification;

import dev.zayt.atrilha.accounts.AccountTestFactory;
import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.auth.domain.PasswordResetResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integração do {@link PasswordResetService} — emissão e verificação
 * de tokens (US-008-a).
 *
 * <p>Cobre os 3 outcomes da verificação (SUCCESS / EXPIRED_OR_INVALID /
 * ALREADY_USED), idempotência de consume, e TTL de 1h na emissão.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PasswordResetServiceIT {

    /** Clock mutável compartilhado entre beans Spring e o teste. */
    static final AtomicReference<Instant> NOW =
            new AtomicReference<>(Instant.parse("2026-05-27T10:00:00Z"));

    @Autowired
    private PasswordResetService service;

    @Autowired
    private PasswordResetTokenRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        NOW.set(Instant.parse("2026-05-27T10:00:00Z"));
    }

    // ---- issueToken ----

    @Test
    void issueToken_invalidaPendentes() {
        // Setup: clock fixo + account existente
        Instant fixedNow = Instant.parse("2026-05-27T10:00:00Z");
        NOW.set(fixedNow);

        Account account = createTestAccount();

        // Token pendente pré-existente
        PasswordResetToken oldToken = new PasswordResetToken();
        oldToken.setId(UUID.randomUUID());
        oldToken.setAccountId(account.getId());
        oldToken.setToken(UUID.randomUUID());
        oldToken.setExpiresAt(fixedNow.plus(Duration.ofHours(2)));
        oldToken.setUsedAt(null);
        oldToken.setCreatedAt(fixedNow.minus(Duration.ofHours(1)));
        UUID oldUuid = oldToken.getToken();
        repository.save(oldToken);

        // Ação
        UUID newUuid = service.issueToken(account);

        // Assert: token antigo foi invalidado
        PasswordResetToken fetchedOld = repository.findByToken(oldUuid).orElseThrow();
        assertThat(fetchedOld.getUsedAt()).isEqualTo(fixedNow);

        // Assert: novo token é diferente e está pendente
        assertThat(newUuid).isNotEqualTo(oldUuid);
        PasswordResetToken newEntity = repository.findByToken(newUuid).orElseThrow();
        assertThat(newEntity.getUsedAt()).isNull();
        assertThat(newEntity.getExpiresAt()).isEqualTo(fixedNow.plus(Duration.ofHours(1)));
        assertThat(newEntity.getAccountId()).isEqualTo(account.getId());
    }

    @Test
    void issueToken_ttl1h() {
        // Setup: clock fixo + account sem tokens pendentes
        Instant fixedNow = Instant.parse("2026-05-27T10:00:00Z");
        NOW.set(fixedNow);

        Account account = createTestAccount();
        repository.deleteByAccountIdAndUsedAtIsNull(account.getId());

        // Ação
        UUID tokenUuid = service.issueToken(account);

        // Assert
        PasswordResetToken entity = repository.findByToken(tokenUuid).orElseThrow();
        assertThat(entity.getExpiresAt()).isEqualTo(Instant.parse("2026-05-27T11:00:00Z"));
        assertThat(entity.getUsedAt()).isNull();
        assertThat(entity.getCreatedAt()).isEqualTo(fixedNow);
    }

    // ---- verify ----

    @Test
    void verify_success() {
        // Setup: token válido, não usado
        Instant fixedNow = Instant.parse("2026-05-27T10:00:00Z");
        NOW.set(fixedNow);

        Account account = createTestAccount();
        UUID tokenUuid = createToken(account, fixedNow.plus(Duration.ofHours(1)), null);

        // Ação
        PasswordResetResult result = service.verify(tokenUuid);

        // Assert: SUCCESS + token marcado como usado
        assertThat(result).isEqualTo(PasswordResetResult.SUCCESS);
        PasswordResetToken entity = repository.findByToken(tokenUuid).orElseThrow();
        assertThat(entity.getUsedAt()).isEqualTo(fixedNow);
    }

    @Test
    void verify_expired() {
        // Setup: token expirado (expires_at no passado)
        Instant fixedNow = Instant.parse("2026-05-27T12:00:00Z");
        NOW.set(fixedNow);

        Account account = createTestAccount();
        UUID tokenUuid = createToken(account, Instant.parse("2026-05-27T11:00:00Z"), null);

        // Ação
        PasswordResetResult result = service.verify(tokenUuid);

        // Assert: EXPIRED_OR_INVALID + used_at permanece null
        assertThat(result).isEqualTo(PasswordResetResult.EXPIRED_OR_INVALID);
        PasswordResetToken entity = repository.findByToken(tokenUuid).orElseThrow();
        assertThat(entity.getUsedAt()).isNull();
    }

    @Test
    void verify_alreadyUsed() {
        // Setup: token ainda válido mas já consumido
        Instant fixedNow = Instant.parse("2026-05-27T10:30:00Z");
        NOW.set(fixedNow);

        Account account = createTestAccount();
        UUID tokenUuid = createToken(account, fixedNow.plus(Duration.ofHours(1)), Instant.parse("2026-05-27T10:00:00Z"));

        // Ação
        PasswordResetResult result = service.verify(tokenUuid);

        // Assert: ALREADY_USED
        assertThat(result).isEqualTo(PasswordResetResult.ALREADY_USED);
    }

    @Test
    void verify_nonExistentUuid() {
        // Setup: nenhum token criado
        UUID randomUuid = UUID.randomUUID();

        // Ação
        PasswordResetResult result = service.verify(randomUuid);

        // Assert: EXPIRED_OR_INVALID (não lança exceção)
        assertThat(result).isEqualTo(PasswordResetResult.EXPIRED_OR_INVALID);
    }

    // ---- consume ----

    @Test
    void consume_idempotent() {
        // Setup: token válido, não usado
        Instant fixedNow = Instant.parse("2026-05-27T10:00:00Z");
        NOW.set(fixedNow);

        Account account = createTestAccount();
        UUID tokenUuid = createToken(account, fixedNow.plus(Duration.ofHours(1)), null);

        // Ação: consumir duas vezes
        service.consume(tokenUuid);
        assertThat(repository.findByToken(tokenUuid).orElseThrow().getUsedAt()).isEqualTo(fixedNow);

        // Segunda chamada não deve lançar exceção
        service.consume(tokenUuid);

        // used_at permanece o mesmo (não avança)
        assertThat(repository.findByToken(tokenUuid).orElseThrow().getUsedAt()).isEqualTo(fixedNow);
    }

    @Test
    void consume_nonExistent_doesNotThrow() {
        // Ação: consumir UUID inexistente não deve lançar exceção
        assertThatCode(() -> service.consume(UUID.randomUUID())).doesNotThrowAnyException();
    }

    // ---- Helpers ----

    private Account createTestAccount() {
        return AccountTestFactory.newAdolescent("test" + UUID.randomUUID() + "@atrilha.dev");
    }

    private UUID createToken(Account account, Instant expiresAt, Instant usedAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setAccountId(account.getId());
        token.setToken(UUID.randomUUID());
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);
        token.setCreatedAt(Instant.parse("2026-05-27T09:00:00Z"));
        repository.save(token);
        return token.getToken();
    }

    /**
     * Configuração de beans mock para testes de integração.
     * Substitui o Clock real por um mutável controlado pelo teste.
     */
    @TestConfiguration
    static class TestBeans {

        @Bean
        @Primary
        Clock clock() {
            return new Clock() {
                @Override
                public ZoneOffset getZone() {
                    return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(java.time.ZoneId zone) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return NOW.get();
                }
            };
        }
    }
}
