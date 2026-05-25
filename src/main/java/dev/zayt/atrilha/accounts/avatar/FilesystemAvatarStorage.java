package dev.zayt.atrilha.accounts.avatar;

import dev.zayt.atrilha.accounts.avatar.AvatarTooLargeException;
import dev.zayt.atrilha.accounts.avatar.AvatarUnsupportedTypeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * Persiste avatar de adolescente no filesystem do container
 * (PRD §9.1, ADR-010). Aceita JPG / PNG / WEBP de até 5 MB, salva em
 * {@code ${app.media.upload-dir}/avatars/{accountId}.{ext}} e expõe URL
 * relativa {@code /media/avatars/{accountId}.{ext}}.
 *
 * <p>Validação minimalista pelo header {@code Content-Type} enviado pelo
 * browser — proteção em profundidade ficaria a cargo de scan/antivírus no
 * pipeline (fora de escopo desta US).</p>
 */
@Component
public final class FilesystemAvatarStorage implements AvatarStorage {

    private static final long MAX_BYTES = 5L * 1024L * 1024L;

    private static final Map<String, String> EXTENSION_BY_MIME = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final Path baseDir;

    public FilesystemAvatarStorage(@Value("${app.media.upload-dir}") String uploadDir) {
        this.baseDir = Paths.get(uploadDir);
    }

    @Override
    public String store(UUID accountId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AvatarUnsupportedTypeException("vazio");
        }
        String contentType = file.getContentType();
        String extension = EXTENSION_BY_MIME.get(contentType == null ? "" : contentType);
        if (extension == null) {
            throw new AvatarUnsupportedTypeException(contentType);
        }
        long size = file.getSize();
        if (size > MAX_BYTES) {
            throw new AvatarTooLargeException(size, MAX_BYTES);
        }

        Path destDir = baseDir.resolve("avatars");
        Path destFile = destDir.resolve(accountId + "." + extension);

        try {
            Files.createDirectories(destDir);
            try (var in = file.getInputStream()) {
                Files.copy(in, destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar avatar", e);
        }

        return "/media/avatars/" + accountId + "." + extension;
    }
}
