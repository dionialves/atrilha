package dev.zayt.atrilha.accounts;

/**
 * Foto enviada tem MIME type fora da lista permitida — JPG, PNG ou WEBP
 * (US-001).
 */
class AvatarUnsupportedTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    AvatarUnsupportedTypeException(String contentType) {
        super("Tipo de arquivo não suportado: " + contentType);
    }
}
