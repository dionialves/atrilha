package dev.zayt.atrilha.auth.domain;

/**
 * Resultado da verificação de um token de e-mail (US-006).
 *
 * <p>{@code EXPIRED_OR_INVALID} unifica "token inexistente" e "token expirado"
 * propositadamente: a UX spec §5.3 exige a mesma tela para os dois casos
 * (privacidade — não revelar se o token existiu).</p>
 */
public enum VerificationResult {
    SUCCESS,
    ALREADY_USED,
    EXPIRED_OR_INVALID
}
