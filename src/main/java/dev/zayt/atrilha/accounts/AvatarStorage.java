package dev.zayt.atrilha.accounts;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Abstração para persistência de avatar de adolescente (US-001).
 *
 * <p>Implementação default vai pra filesystem do container
 * ({@link FilesystemAvatarStorage}). A interface fica explícita para que o
 * service possa ser testado com mock e para que migrar para S3/CDN seja
 * troca de bean (não de chamadas espalhadas).</p>
 */
interface AvatarStorage {

    /**
     * Persiste o conteúdo do {@code file} associado à conta {@code accountId}.
     *
     * @return URL relativa para o avatar (ex.: {@code /media/avatars/{id}.jpg}).
     * @throws AvatarTooLargeException se o arquivo exceder o limite de tamanho.
     * @throws AvatarUnsupportedTypeException se o MIME type não for suportado.
     */
    String store(UUID accountId, MultipartFile file);
}
