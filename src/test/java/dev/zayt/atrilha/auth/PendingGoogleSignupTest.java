package dev.zayt.atrilha.auth;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PendingGoogleSignup} viaja na sessao HTTP entre o callback OAuth e
 * o POST de complementacao (US-002). A sessao serializa atributos durante
 * replicacao/persistencia, entao o record precisa ser efetivamente
 * Serializable — em particular {@link OffsetDateTime} e {@link Instant}
 * sao Serializable, mas qualquer campo nao-serializable quebra o round-trip.
 */
class PendingGoogleSignupTest {

    @Test
    void roundTripsThroughJavaSerialization() throws Exception {
        PendingGoogleSignup original = new PendingGoogleSignup(
                "julia@gmail.com",
                OffsetDateTime.of(2026, 5, 20, 12, 0, 0, 0, ZoneOffset.UTC),
                "Julia",
                "https://lh3.googleusercontent.com/a/julia",
                Instant.parse("2026-05-20T12:00:00Z"));

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(buf)) {
            oos.writeObject(original);
        }
        PendingGoogleSignup restored;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buf.toByteArray()))) {
            restored = (PendingGoogleSignup) ois.readObject();
        }

        assertThat(restored).isEqualTo(original);
        assertThat(restored.email()).isEqualTo("julia@gmail.com");
        assertThat(restored.emailVerifiedAt()).isEqualTo(original.emailVerifiedAt());
        assertThat(restored.givenName()).isEqualTo("Julia");
        assertThat(restored.picture()).isEqualTo(original.picture());
        assertThat(restored.createdAt()).isEqualTo(original.createdAt());
    }
}
