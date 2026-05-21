package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.auth.config.LoginRateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private Clock fixedClock;
    private Instant baseTime;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        baseTime = Instant.parse("2026-01-01T00:00:00Z");
        fixedClock = Clock.fixed(baseTime, ZoneId.of("UTC"));
        LoginRateLimitProperties props = LoginRateLimitProperties.defaults();
        service = new LoginAttemptService(props, fixedClock);
    }

    // ---- Critério: chave é normalizada por email lowercase e IP ----

    @Test
    @DisplayName("chave é normalizada: email lowercase + ip")
    void chaveENormalizadaPorEmailLowercaseEPorIp() {
        LoginAttemptKey key1 = LoginAttemptKey.of("Ana@X.com", "1.2.3.4");
        LoginAttemptKey key2 = LoginAttemptKey.of("ana@x.com", "1.2.3.4");
        LoginAttemptKey key3 = LoginAttemptKey.of("  ANA@X.COM  ", "1.2.3.4");

        assertEquals(key1, key2);
        assertEquals(key1, key3);
        assertEquals(key1.hashCode(), key2.hashCode());
        assertEquals(key1.hashCode(), key3.hashCode());
    }

    // ---- Critério: registerFailure incrementa contador e bloqueia ----

    @Test
    @DisplayName("registraFalha incrementa contador até maxAttempts")
    void registraFalhaIncrementaContador() {
        LoginAttemptKey key = LoginAttemptKey.of("user@test.com", "10.0.0.1");

        assertFalse(service.isBlocked(key));
        service.registerFailure(key);
        assertFalse(service.isBlocked(key)); // 1/5
        service.registerFailure(key);
        assertFalse(service.isBlocked(key)); // 2/5
        service.registerFailure(key);
        assertFalse(service.isBlocked(key)); // 3/5
        service.registerFailure(key);
        assertFalse(service.isBlocked(key)); // 4/5
        service.registerFailure(key);
        assertTrue(service.isBlocked(key));  // 5/5 — bloqueado!
    }

    // ---- Critério: registerSuccess zera estado ----

    @Test
    @DisplayName("sucessoLimpaContador")
    void sucessoLimpaContador() {
        LoginAttemptKey key = LoginAttemptKey.of("user@test.com", "10.0.0.2");

        service.registerFailure(key);
        service.registerFailure(key);
        assertFalse(service.isBlocked(key)); // 2/5

        service.registerSuccess(key);       // limpa
        assertFalse(service.isBlocked(key)); // resetado

        service.registerFailure(key);       // 1/5 novamente
        assertFalse(service.isBlocked(key));
    }

    // ---- Critério: janela expira e contador zera (Clock mockado) ----

    @Test
    @DisplayName("expiraContagemAposJanela")
    void expiraContagemAposJanela() {
        LoginRateLimitProperties props = new LoginRateLimitProperties(5, Duration.ofMinutes(1), Duration.ofMinutes(1));
        InstantHolder holder = new InstantHolder(baseTime);

        service = new LoginAttemptService(props, makeAdvancingClock(holder));

        LoginAttemptKey key = LoginAttemptKey.of("user@test.com", "10.0.0.3");

        service.registerFailure(key);
        service.registerFailure(key);
        assertFalse(service.isBlocked(key));

        // Avança o clock além da janela (1 min)
        holder.advance(Duration.ofSeconds(65));

        // isBlocked deve limpar o estado antigo
        assertFalse(service.isBlocked(key)); // janela expirou, contagem zerada

        service.registerFailure(key);
        assertFalse(service.isBlocked(key)); // 1/5 novamente
    }

    @Nested
    @DisplayName("Bordas")
    class EdgeCases {

        // ---- Bloqueio expira após blockDuration (Clock mockado) ----

        @Test
        @DisplayName("bloqueio expira após blockDuration")
        void bloqueioExpiraAposBlockDuration() {
            LoginRateLimitProperties props = new LoginRateLimitProperties(3, Duration.ofMinutes(60), Duration.ofSeconds(2));

            // Clock mutável para simular passagem de tempo
            InstantHolder holder = new InstantHolder(baseTime);

            service = new LoginAttemptService(props, makeAdvancingClock(holder));
            LoginAttemptKey key = LoginAttemptKey.of("user@test.com", "10.0.0.4");

            // 3 falhas = bloqueado
            service.registerFailure(key);
            service.registerFailure(key);
            service.registerFailure(key);
            assertTrue(service.isBlocked(key));

            // Avança além do blockDuration (2s)
            holder.advance(Duration.ofSeconds(3));

            // Bloqueio expirou
            assertFalse(service.isBlocked(key));
        }

        // ---- Email diferente no mesmo IP não bloqueia ----

        @Test
        @DisplayName("5 falhas com email A em IP X não bloqueia email B no mesmo IP")
        void emailsDiferentesNoMesmoIp() {
            LoginAttemptKey keyA = LoginAttemptKey.of("ana@test.com", "10.0.0.5");
            LoginAttemptKey keyB = LoginAttemptKey.of("beto@test.com", "10.0.0.5");

            service.registerFailure(keyA);
            service.registerFailure(keyA);
            service.registerFailure(keyA);
            service.registerFailure(keyA);
            service.registerFailure(keyA);

            assertTrue(service.isBlocked(keyA));  // ana bloqueada
            assertFalse(service.isBlocked(keyB)); // beto não — chave diferente!
        }

        // ---- Tenta registrar falha quando já bloqueado ----

        @Test
        @DisplayName("registraFalha não altera estado quando já bloqueado")
        void registraFalhaJaBloqueado() {
            LoginRateLimitProperties props = new LoginRateLimitProperties(2, Duration.ofMinutes(60), Duration.ofMinutes(30));
            service = new LoginAttemptService(props, fixedClock);

            LoginAttemptKey key = LoginAttemptKey.of("user@test.com", "10.0.0.6");

            service.registerFailure(key);
            service.registerFailure(key);
            assertTrue(service.isBlocked(key));

            // Mais tentativas — continua bloqueado (não reseta)
            service.registerFailure(key);
            assertTrue(service.isBlocked(key));
        }

        // ---- Chave com IP diferente não interfere ----

        @Test
        @DisplayName("mesmo email, IPs diferentes são independentes")
        void mesmoEmailIPsDiferentes() {
            LoginAttemptKey key1 = LoginAttemptKey.of("user@test.com", "10.0.0.7");
            LoginAttemptKey key2 = LoginAttemptKey.of("user@test.com", "10.0.0.8");

            service.registerFailure(key1);
            service.registerFailure(key1);
            service.registerFailure(key1);
            service.registerFailure(key1);
            service.registerFailure(key1);

            assertTrue(service.isBlocked(key1));
            assertFalse(service.isBlocked(key2)); // IP diferente — independente!
        }

        // ---- registerSuccess remove mesmo quando bloqueado ----

        @Test
        @DisplayName("registerSuccess desbloqueia mesmo após maxAttempts")
        void sucessoDesbloqueia() {
            LoginRateLimitProperties props = new LoginRateLimitProperties(2, Duration.ofMinutes(60), Duration.ofMinutes(30));
            service = new LoginAttemptService(props, fixedClock);

            LoginAttemptKey key = LoginAttemptKey.of("user@test.com", "10.0.0.9");

            service.registerFailure(key);
            service.registerFailure(key);
            assertTrue(service.isBlocked(key));

            // Sucesso remove a chave — desbloqueia
            service.registerSuccess(key);
            assertFalse(service.isBlocked(key));
        }

        // ---- Default properties ----

        @Test
        @DisplayName("properties com defaults se ausentes")
        void propertiesDefaults() {
            LoginRateLimitProperties props = LoginRateLimitProperties.defaults();

            assertEquals(5, props.maxAttempts());
            assertEquals(Duration.ofMinutes(15), props.attemptWindow());
            assertEquals(Duration.ofMinutes(15), props.blockDuration());
        }

        // ---- registerSuccess em chave inexistente é noop ----

        @Test
        @DisplayName("registerSuccess em chave inexistente não lança exceção")
        void sucessoEmChaveInexistente() {
            LoginAttemptKey key = LoginAttemptKey.of("ghost@test.com", "10.0.0.99");
            assertDoesNotThrow(() -> service.registerSuccess(key));
        }

        // ---- isBlocked em chave inexistente retorna false ----

        @Test
        @DisplayName("isBlocked em chave inexistente retorna false")
        void isBlockedInexistente() {
            LoginAttemptKey key = LoginAttemptKey.of("ghost@test.com", "10.0.0.98");
            assertFalse(service.isBlocked(key));
        }
    }

    // ---- Helper: Clock mutável para simular passagem de tempo ----

    static class InstantHolder {
        private Instant now;

        InstantHolder(Instant initial) { this.now = initial; }

        Instant get() { return now; }

        void advance(Duration d) { this.now = this.now.plus(d); }
    }

    private static Clock makeAdvancingClock(InstantHolder holder) {
        return new Clock() {
            @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
            @Override public Clock withZone(ZoneId zone) { return this; }
            @Override public Instant instant() { return holder.get(); }
        };
    }
}
