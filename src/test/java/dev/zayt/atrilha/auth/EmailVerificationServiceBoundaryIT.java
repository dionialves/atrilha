package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.AccountTestFactory;
import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.auth.verification.EmailVerificationToken;
import dev.zayt.atrilha.auth.verification.EmailVerificationTokenRepository;
import dev.zayt.atrilha.auth.verification.EmailVerificationService;
import dev.zayt.atrilha.auth.exception.EmailResendRateLimitedException;
import dev.zayt.atrilha.auth.domain.VerificationResult;
import dev.zayt.atrilha.notifications.RecordingEmailSender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de borda complementares para o {@link EmailVerificationService}
 * (QA — não substitui os ITs do Codificador). Foca em:
 * <ul>
 *   <li>Cooldown exato: 59s falha; 60s passa.</li>
 *   <li>Limite por hora exato: 5º passa; 6º falha.</li>
 *   <li>Token pertencente a outra conta — service verifica o dono do token,
 *       não o usuário logado.</li>
 *   <li>Verify concorrente do mesmo token — apenas um {@code SUCCESS};
 *       outra(s) chamada(s) recebem {@code ALREADY_USED} (idempotência sob
 *       contenção).</li>
 * </ul>
 *
 * <p>Justificativa: o IT atual usa 30s / 61s e 6 reenvios em loop — bom para
 * caminho feliz/infeliz, mas não exercita a borda exata e não cobre
 * contenção concorrente. Race condition aqui falha = sistema entrega
 * "duplo SUCCESS" e potencialmente duplo redirect, sinal de bug grave.</p>
 */
@Testcontainers
@SpringBootTest(classes = { AtrilhaApplication.class, EmailVerificationServiceBoundaryIT.TestBeans.class },
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@ActiveProfiles("test")
@DirtiesContext
class EmailVerificationServiceBoundaryIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("atrilha")
                    .withUsername("atrilha")
                    .withPassword("atrilha");

    @DynamicPropertySource
    static void registerPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** Clock mutável compartilhado entre beans Spring e o teste. */
    static final AtomicReference<Instant> NOW =
            new AtomicReference<>(Instant.parse("2026-05-19T12:00:00Z"));

    @TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
        }

        @Bean
        @Primary
        Clock movableClock() {
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

    @Autowired
    EmailVerificationService service;

    @Autowired
    EmailVerificationTokenRepository tokenRepository;

    @Autowired
    EntityManager em;

    @Autowired
    RecordingEmailSender mailer;

    @Autowired
    PlatformTransactionManager txManager;

    TransactionTemplate tx;

    @BeforeEach
    void resetEverything() {
        NOW.set(Instant.parse("2026-05-19T12:00:00Z"));
        mailer.clear();
        tx = new TransactionTemplate(txManager);
    }

    private Account persistedAccount(String email) {
        Account a = AccountTestFactory.newAdolescent(email);
        tx.executeWithoutResult(status -> em.persist(a));
        return a;
    }

    // ---------- Cooldown 60s — borda exata ----------

    @Test
    void resend_at59SecondsAfterPrevious_throwsRateLimited() {
        Account a = persistedAccount("cd-59@example.com");
        service.resend(a);
        // 59 segundos depois — última nanosegundo antes do cooldown expirar.
        NOW.set(NOW.get().plus(59, ChronoUnit.SECONDS));

        assertThatThrownBy(() -> service.resend(a))
                .as("59s ainda dentro do cooldown — deve bloquear")
                .isInstanceOf(EmailResendRateLimitedException.class);
    }

    @Test
    void resend_at60SecondsAfterPrevious_succeeds() {
        Account a = persistedAccount("cd-60@example.com");
        service.resend(a);
        // 60 segundos cravados — cooldown expira (Duration.between == 60s).
        NOW.set(NOW.get().plus(60, ChronoUnit.SECONDS));

        // Não deve lançar.
        service.resend(a);

        assertThat(mailer.recorded())
                .as("aos 60s o reenvio passa — 2 e-mails registrados")
                .hasSize(2);
    }

    // ---------- Limite por hora — borda exata 5/6 ----------

    @Test
    void resend_fifthInSameHour_succeeds_sixthFails() {
        Account a = persistedAccount("rl-5and6@example.com");
        // 5 reenvios espaçados de 61s cada (todos dentro de 1h).
        for (int i = 0; i < 5; i++) {
            service.resend(a);
            NOW.set(NOW.get().plus(61, ChronoUnit.SECONDS));
        }
        assertThat(mailer.recorded())
                .as("5 reenvios — todos passam (limite EXATO permitido)")
                .hasSize(5);

        // 6º — tem que falhar por limite por hora.
        assertThatThrownBy(() -> service.resend(a))
                .as("6º reenvio na mesma hora deve bloquear")
                .isInstanceOf(EmailResendRateLimitedException.class);

        // Avança 1 hora completa — janela rolou; 6º deve passar.
        NOW.set(NOW.get().plus(60, ChronoUnit.MINUTES));
        service.resend(a);
        assertThat(mailer.recorded())
                .as("após janela rolar, novo reenvio passa")
                .hasSize(6);
    }

    // ---------- Token de outra conta — comportamento do contrato ----------

    @Test
    void verify_tokenOfDifferentAccount_marksOwnerVerifiedNotCaller() {
        // O service não conhece "usuário logado" — opera estritamente sobre o
        // dono do token. Esse teste documenta o contrato: o token é a única
        // credencial; quem clica não importa.
        Account owner = persistedAccount("owner@example.com");
        Account stranger = persistedAccount("stranger@example.com");

        // Emite token para `owner`.
        UUID token = service.issueToken(owner);

        // Stranger (ou anonymous) chama verify — verifica a conta do owner.
        VerificationResult result = service.verify(token);

        assertThat(result).isEqualTo(VerificationResult.SUCCESS);
        Account refreshedOwner = em.find(Account.class, owner.getId());
        Account refreshedStranger = em.find(Account.class, stranger.getId());
        assertThat(refreshedOwner.getEmailVerifiedAt())
                .as("conta do owner do token deve ter sido verificada")
                .isNotNull();
        assertThat(refreshedStranger.getEmailVerifiedAt())
                .as("conta do stranger NÃO deve ser tocada")
                .isNull();
    }

    // ---------- Race condition: dois verifies do mesmo token ----------

    /**
     * <p>Duas threads chamando {@link EmailVerificationService#verify(UUID)}
     * simultaneamente com o mesmo token devem produzir <strong>exatamente um
     * {@link VerificationResult#SUCCESS}</strong>. A segunda chamada vê o token
     * já marcado e retorna {@link VerificationResult#ALREADY_USED}
     * (idempotência sob contenção).</p>
     *
     * <p>Garantia obtida via {@code @Lock(LockModeType.PESSIMISTIC_WRITE)} no
     * {@code findByToken(UUID)} do repositório: a primeira transação adquire
     * lock de linha; a segunda bloqueia até a primeira commitar, e então
     * enxerga {@code used_at != null}.</p>
     */
    @Test
    void verify_concurrentCallsOnSameToken_onlyOneSucceeds() throws Exception {
        Account a = persistedAccount("race@example.com");
        UUID token = service.issueToken(a);

        // Duas threads chamam verify ao mesmo tempo.
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch fire = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicReference<VerificationResult> r1 = new AtomicReference<>();
        AtomicReference<VerificationResult> r2 = new AtomicReference<>();
        AtomicReference<Throwable> err1 = new AtomicReference<>();
        AtomicReference<Throwable> err2 = new AtomicReference<>();

        pool.submit(() -> {
            try {
                ready.countDown();
                fire.await();
                r1.set(service.verify(token));
            } catch (Throwable t) {
                err1.set(t);
            }
        });
        pool.submit(() -> {
            try {
                ready.countDown();
                fire.await();
                r2.set(service.verify(token));
            } catch (Throwable t) {
                err2.set(t);
            }
        });

        ready.await(5, TimeUnit.SECONDS);
        fire.countDown();
        pool.shutdown();
        boolean done = pool.awaitTermination(15, TimeUnit.SECONDS);
        assertThat(done).as("threads terminam dentro do timeout").isTrue();

        // Nenhuma deve ter explodido com exception inesperada — comportamento
        // determinístico: ambas retornam algo do enum.
        if (err1.get() != null) throw new AssertionError("thread 1 falhou", err1.get());
        if (err2.get() != null) throw new AssertionError("thread 2 falhou", err2.get());

        List<VerificationResult> results = List.of(r1.get(), r2.get());
        long successes = results.stream().filter(r -> r == VerificationResult.SUCCESS).count();
        long alreadyUsed = results.stream().filter(r -> r == VerificationResult.ALREADY_USED).count();
        long expiredOrInvalid = results.stream().filter(r -> r == VerificationResult.EXPIRED_OR_INVALID).count();

        // Ambas as chamadas retornam um outcome conhecido (sem null/exception),
        // e somente UMA pode reportar SUCCESS — a outra vê used_at != null
        // após o lock liberar e retorna ALREADY_USED.
        assertThat(successes + alreadyUsed + expiredOrInvalid)
                .as("ambas as chamadas retornam um outcome conhecido do enum (sem null)")
                .isEqualTo(2);
        assertThat(successes)
                .as("apenas uma thread pode marcar o token como usado")
                .isEqualTo(1L);
        assertThat(alreadyUsed)
                .as("a thread perdedora vê o token já consumido")
                .isEqualTo(1L);

        // CONSISTÊNCIA do estado final (esses asserts continuam valendo
        // mesmo após o fix da concorrência — são regressão verdadeira).
        EmailVerificationToken finalToken = tokenRepository.findByToken(token).orElseThrow();
        assertThat(finalToken.getUsedAt())
                .as("token sempre fica marcado após qualquer verify bem-sucedido")
                .isNotNull();
        Account refreshed = em.find(Account.class, a.getId());
        assertThat(refreshed.getEmailVerifiedAt())
                .as("conta foi verificada (idempotência ao menos no estado final)")
                .isNotNull();
    }

    // ---------- Idempotência: verify chamado 2× em sequência ----------

    @Test
    void verify_calledTwiceSequentially_firstSuccess_secondAlreadyUsed() {
        Account a = persistedAccount("idem@example.com");
        UUID token = service.issueToken(a);

        VerificationResult first = service.verify(token);
        VerificationResult second = service.verify(token);

        assertThat(first).isEqualTo(VerificationResult.SUCCESS);
        assertThat(second)
                .as("segunda chamada com o mesmo token NUNCA retorna SUCCESS")
                .isEqualTo(VerificationResult.ALREADY_USED);

        // E o timestamp não foi sobrescrito por nenhuma das chamadas
        // subsequentes (idempotência completa).
        Account refreshed = em.find(Account.class, a.getId());
        OffsetDateTime firstTimestamp = refreshed.getEmailVerifiedAt();
        service.verify(token);
        Account refreshedAgain = em.find(Account.class, a.getId());
        assertThat(refreshedAgain.getEmailVerifiedAt())
                .as("timestamp original preservado mesmo após N chamadas")
                .isEqualTo(firstTimestamp);
    }

    // ---------- Token expirado por 1 segundo — limite exato ----------

    @Test
    void verify_tokenExpiredByOneSecond_returnsExpired() {
        Account a = persistedAccount("expired-edge@example.com");
        // Constrói manualmente um token que expirou há exatamente 1s.
        EmailVerificationToken edge = new EmailVerificationToken();
        edge.setId(UUID.randomUUID());
        edge.setAccountId(a.getId());
        edge.setToken(UUID.randomUUID());
        edge.setExpiresAt(NOW.get().minusSeconds(1));
        edge.setCreatedAt(NOW.get().minus(24, ChronoUnit.HOURS).minusSeconds(1));
        tx.executeWithoutResult(status -> tokenRepository.saveAndFlush(edge));

        VerificationResult r = service.verify(edge.getToken());

        assertThat(r).isEqualTo(VerificationResult.EXPIRED_OR_INVALID);
        Account refreshed = em.find(Account.class, a.getId());
        assertThat(refreshed.getEmailVerifiedAt())
                .as("token expirado não verifica a conta")
                .isNull();
    }
}
