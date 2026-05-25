package dev.zayt.atrilha.accounts.avatar;

/**
 * Foto enviada excede o limite de 5 MB (US-001).
 */
public class AvatarTooLargeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AvatarTooLargeException(long actualBytes, long maxBytes) {
        super("Avatar excede o tamanho máximo: " + actualBytes + " bytes (limite " + maxBytes + ")");
    }
}
