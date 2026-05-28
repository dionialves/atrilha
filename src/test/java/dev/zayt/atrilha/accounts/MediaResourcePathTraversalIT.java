package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.testsupport.AbstractSpringPostgresIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint {@code /media/**} serve avatares do filesystem do container
 * ({@link MediaResourceConfig}). Esta suíte verifica dois contratos
 * estruturais essenciais:
 *
 * <ul>
 *   <li>Arquivos válidos existentes são servidos com 200.</li>
 *   <li>Tentativas de path traversal (escapar do {@code upload-dir}) NÃO
 *       resultam em vazamento de arquivos do host. Como {@code /media/**}
 *       é público (sem auth), regressão aqui é exposição direta.</li>
 * </ul>
 *
 * <p>Spring's {@code ResourceHttpRequestHandler} já rejeita {@code ../} por
 * default; este teste é um guardrail contra alguém substituir por um handler
 * customizado sem o mesmo filtro.</p>
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.baseline-on-migrate=false"
})
@ActiveProfiles("test")
class MediaResourcePathTraversalIT extends AbstractSpringPostgresIT {

    @Autowired
    WebApplicationContext ctx;

    @Value("${app.media.upload-dir}")
    String uploadDir;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    /**
     * Smoke positivo: um arquivo plantado dentro do upload-dir/avatars é
     * servido com 200 e Content-Type de imagem. Garante que o handler está
     * de fato roteando antes de testar os negativos.
     */
    @Test
    void servesExistingAvatarFile() throws Exception {
        Path avatarsDir = Paths.get(uploadDir).resolve("avatars");
        Files.createDirectories(avatarsDir);
        Path file = avatarsDir.resolve("test-avatar-present.jpg");
        Files.write(file, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3});
        try {
            mvc.perform(get("/media/avatars/test-avatar-present.jpg"))
                    .andExpect(status().isOk());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /**
     * Tentativa clássica de path traversal: {@code /media/avatars/../../etc/passwd}.
     * O handler NÃO pode retornar 200 e NÃO pode servir conteúdo de fora do
     * upload-dir. Aceita 400/404/403 — qualquer não-2xx é seguro.
     */
    @Test
    void rejectsTraversalAttemptWithDotDotSegments() throws Exception {
        mvc.perform(get("/media/avatars/../../../../../../etc/passwd"))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    assertThat(statusCode < 200 || statusCode >= 300)
                            .as("path traversal não pode retornar 2xx — recebeu %d", statusCode)
                            .isTrue();
                    String body = result.getResponse().getContentAsString();
                    assertThat(body)
                            .as("corpo da resposta não pode conter dados de /etc/passwd")
                            .doesNotContain("root:")
                            .doesNotContain("/bin/bash")
                            .doesNotContain("/bin/sh");
                });
    }

    /**
     * Path traversal codificado em URL ({@code %2e%2e}) — o handler precisa
     * normalizar antes de decidir.
     */
    @Test
    void rejectsTraversalAttemptWithEncodedDotDotSegments() throws Exception {
        mvc.perform(get("/media/avatars/%2e%2e/%2e%2e/etc/passwd"))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    assertThat(statusCode < 200 || statusCode >= 300)
                            .as("traversal codificado não pode retornar 2xx — recebeu %d", statusCode)
                            .isTrue();
                });
    }

    /**
     * Arquivo absoluto fora do prefixo {@code /media/avatars}. Como
     * {@code /media/**} mapeia apenas para o {@code upload-dir}, qualquer
     * caminho que tente escapar via /// ou caracteres absolutos deve falhar.
     */
    @Test
    void rejectsRequestForFilenameNotInUploadDir() throws Exception {
        // Plantamos um arquivo FORA de upload-dir/avatars — em /tmp diretamente
        // — e tentamos buscá-lo pelo nome. O handler está bound a uploadDir,
        // então essa busca não pode resolver.
        Path outsideFile = Paths.get(System.getProperty("java.io.tmpdir"))
                .resolve("atrilha-outside-test-secret.txt");
        Files.write(outsideFile, "secret-do-not-leak".getBytes());
        try {
            mvc.perform(get("/media/" + outsideFile.getFileName()))
                    .andExpect(result -> {
                        int statusCode = result.getResponse().getStatus();
                        assertThat(statusCode < 200 || statusCode >= 300)
                                .as("arquivo fora de upload-dir não pode ser servido (status %d)", statusCode)
                                .isTrue();
                        String body = result.getResponse().getContentAsString();
                        assertThat(body).doesNotContain("secret-do-not-leak");
                    });
        } finally {
            Files.deleteIfExists(outsideFile);
        }
    }

    /**
     * Filename inexistente dentro de /media/avatars retorna 404 (não 5xx).
     */
    @Test
    void unknownAvatarFilenameReturns404() throws Exception {
        mvc.perform(get("/media/avatars/nope-this-uuid-does-not-exist.jpg"))
                .andExpect(status().isNotFound());
    }
}
