package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.accounts.avatar.FilesystemAvatarStorage;
import dev.zayt.atrilha.accounts.avatar.AvatarTooLargeException;
import dev.zayt.atrilha.accounts.avatar.AvatarUnsupportedTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Armazenamento de avatar via filesystem do container (ADR-010, PRD §9.1).
 *
 * <p>Contrato (US-001):
 * <ul>
 *   <li>Aceita JPG / PNG / WEBP, ≤ 5 MB.</li>
 *   <li>Salva em {@code ${upload-dir}/avatars/{accountId}.{ext}} preservando a extensão.</li>
 *   <li>Retorna URL relativa {@code /media/avatars/{accountId}.{ext}}.</li>
 *   <li>Rejeita arquivos maiores ou de tipo MIME não suportado com exceções dedicadas.</li>
 * </ul>
 * </p>
 */
class FilesystemAvatarStorageTest {

    @TempDir
    Path tempDir;

    private FilesystemAvatarStorage storage;

    @BeforeEach
    void setUp() {
        storage = new FilesystemAvatarStorage(tempDir.toString());
    }

    @Test
    void storesJpegAndReturnsRelativeUrl() throws Exception {
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        MockMultipartFile file = new MockMultipartFile(
                "photo", "selfie.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3});

        String url = storage.store(id, file);

        assertThat(url).isEqualTo("/media/avatars/" + id + ".jpg");
        Path stored = tempDir.resolve("avatars").resolve(id + ".jpg");
        assertThat(Files.exists(stored)).isTrue();
        assertThat(Files.size(stored)).isEqualTo(7);
    }

    @Test
    void storesPngWithProperExtension() throws Exception {
        UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        MockMultipartFile file = new MockMultipartFile(
                "photo", "selfie.png", "image/png", new byte[]{1, 2, 3});

        String url = storage.store(id, file);

        assertThat(url).isEqualTo("/media/avatars/" + id + ".png");
        assertThat(Files.exists(tempDir.resolve("avatars").resolve(id + ".png"))).isTrue();
    }

    @Test
    void storesWebpWithProperExtension() throws Exception {
        UUID id = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        MockMultipartFile file = new MockMultipartFile(
                "photo", "selfie.webp", "image/webp", new byte[]{1, 2, 3});

        String url = storage.store(id, file);

        assertThat(url).isEqualTo("/media/avatars/" + id + ".webp");
    }

    @Test
    void rejectsFileLargerThan5MB() {
        UUID id = UUID.randomUUID();
        byte[] tooBig = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "photo", "huge.jpg", "image/jpeg", tooBig);

        assertThatThrownBy(() -> storage.store(id, file))
                .isInstanceOf(AvatarTooLargeException.class);
    }

    @Test
    void acceptsFileExactlyAt5MB() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] exactlyFive = new byte[5 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "photo", "edge.jpg", "image/jpeg", exactlyFive);

        String url = storage.store(id, file);

        assertThat(url).contains(id.toString());
    }

    @Test
    void rejectsUnsupportedMimeType() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "photo", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> storage.store(id, file))
                .isInstanceOf(AvatarUnsupportedTypeException.class);
    }

    @Test
    void rejectsGifMimeType() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "photo", "animated.gif", "image/gif", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> storage.store(id, file))
                .isInstanceOf(AvatarUnsupportedTypeException.class);
    }

    @Test
    void rejectsEmptyFile() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "photo", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> storage.store(id, file))
                .isInstanceOf(AvatarUnsupportedTypeException.class);
    }
}
