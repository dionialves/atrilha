package dev.zayt.atrilha.auth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrail estrutural para CA-5 da US-005 (Issue #36):
 * <em>"Nenhuma das tentativas bloqueadas gera e-mail de verificação nem deixa
 * rastros visíveis ao usuário (sem 'conta pendente')."</em>
 *
 * <p>O contrato é: o validador é função pura — não cria entidade JPA, não
 * dispara e-mail, não persiste nada. Esse teste falha se alguém adicionar
 * persistência ou envio de e-mail dentro do pacote {@code auth} de forma
 * acoplada ao fluxo do validador.</p>
 *
 * <p>Implementação: varre o pacote {@code src/main/java/dev/zayt/atrilha/auth}
 * procurando por marcadores Spring/JPA/Mail proibidos nesta US. Se a US-001+
 * adicionar persistência de conta, esse teste precisará ser revisitado —
 * mas isso é justamente a intenção: a quebra força revisão consciente.</p>
 *
 * <p>Não testa código de produção em si; testa o conjunto de arquivos
 * tratados como contrato da feature (separação de responsabilidades).</p>
 */
class AgeEligibilityNoTraceTest {

    private static final Path AUTH_SOURCE_DIR =
            Paths.get("src/main/java/dev/zayt/atrilha/auth").toAbsolutePath();

    private static final List<String> FORBIDDEN_MARKERS = List.of(
            "@Entity",
            "@Table",
            "JpaRepository",
            "CrudRepository",
            "JavaMailSender",
            "@Repository",
            "EntityManager"
    );

    @Test
    void authPackage_doesNotIntroducePersistenceOrMailWhileScopeIsUs005()
            throws IOException {
        assertThat(AUTH_SOURCE_DIR).exists().isDirectory();

        try (Stream<Path> files = Files.walk(AUTH_SOURCE_DIR)) {
            List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();

            assertThat(javaFiles)
                    .as("Pacote auth deve ter os arquivos da US-005")
                    .isNotEmpty();

            for (Path file : javaFiles) {
                String content = Files.readString(file);
                for (String marker : FORBIDDEN_MARKERS) {
                    assertThat(content)
                            .as("CA-5 da US-005: arquivo %s não deve conter marcador de "
                                    + "persistência/mail '%s' enquanto o escopo for apenas "
                                    + "o validador. Se a US-001+ adicionou persistência, "
                                    + "este teste deve ser revisado em conjunto com aquela US.",
                                    AUTH_SOURCE_DIR.relativize(file), marker)
                            .doesNotContain(marker);
                }
            }
        }
    }

    @Test
    void ageEligibilityChecker_doesNotLogBirthDateOrAge() throws IOException {
        // PII: a US declara explicitamente que birthDate e idade calculada
        // NÃO devem ser logados (LGPD + ADR-006). Esse teste confere que o
        // código do checker não imprime nem usa Logger com a variável.
        Path checker = AUTH_SOURCE_DIR.resolve("AgeEligibilityChecker.java");
        String content = Files.readString(checker);

        assertThat(content)
                .as("AgeEligibilityChecker não deve importar SLF4J/Logger")
                .doesNotContain("org.slf4j.Logger")
                .doesNotContain("LoggerFactory");

        assertThat(content)
                .as("AgeEligibilityChecker não deve usar System.out/err")
                .doesNotContain("System.out")
                .doesNotContain("System.err");
    }
}
