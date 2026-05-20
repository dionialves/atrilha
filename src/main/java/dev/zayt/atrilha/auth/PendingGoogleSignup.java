package dev.zayt.atrilha.auth;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Estado de cadastro Google em andamento, armazenado na sessao HTTP entre
 * o callback OAuth (OAuthSuccessHandler) e o POST de complementacao
 * (AdolescentGoogleSignupController). US-002.
 *
 * <p>Carrega tudo o que o Google ja entregou e que o produto vai persistir
 * (e-mail verificado, sugestao de apelido, foto) + um {@code createdAt}
 * tecnico para futuras politicas de expiracao (nao usado neste sprint).</p>
 *
 * <p>{@code public} porque o controller vive no pacote {@code accounts} e
 * le este atributo da sessao. {@link Serializable} porque a sessao
 * Spring HTTP serializa atributos quando ha replicacao/persistencia.</p>
 */
public record PendingGoogleSignup(
        String email,
        OffsetDateTime emailVerifiedAt,
        String givenName,
        String picture,
        Instant createdAt) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
