package dev.zayt.atrilha.accounts.avatar;

/**
 * Foto enviada tem MIME type fora da lista permitida — JPG, PNG ou WEBP
 * (US-001).
 */
public class AvatarUnsupportedTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AvatarUnsupportedTypeException(String contentType) {
        super("Tipo de arquivo não suportado: " + contentType);
    }
}
