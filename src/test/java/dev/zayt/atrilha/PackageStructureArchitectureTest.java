package dev.zayt.atrilha;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Teste de arquitetura — estrutura de pacotes por bounded context.
 *
 * Usa reflection pura (sem ArchUnit) para travar que imports cruzados
 * indesejados não regressem. Allowlists documentam exceções legítimas.
 *
 * <p>Allowlist accounts → auth (classes que accounts pode importar de auth):</p>
 * <ul>
 *   <li>{@code SessionAuthenticator} — porta de autenticação, consumida por services</li>
 * </ul>
 *
 * <p>Allowlist auth → accounts (classes que auth pode importar de accounts):</p>
 * <ul>
 *   <li>{@code Account} — entidade de leitura</li>
 *   <li>{@code AccountReader}, {@code AccountProfileLookup} — interfaces de leitura (módulo accounts.repository)</li>
 * </ul>
 */
class PackageStructureArchitectureTest {

    private static final String ROOT_PACKAGE = "dev.zayt.atrilha";
    private static final String ACCOUNTS_PKG = ROOT_PACKAGE + ".accounts";
    private static final String AUTH_PKG = ROOT_PACKAGE + ".auth";

    // Allowlist accounts → auth: classes que accounts é permitido importar de auth
    private static final Set<String> ACCOUNTS_TO_AUTH_ALLOWLIST = Set.of(
            "dev.zayt.atrilha.auth.SessionAuthenticator"
    );

    // Allowlist auth → accounts: classes que auth é permitido importar de accounts
    private static final Set<String> AUTH_TO_ACCOUNTS_ALLOWLIST = Set.of(
            "dev.zayt.atrilha.accounts.Account",
            "dev.zayt.atrilha.accounts.repository.AccountReader",
            "dev.zayt.atrilha.accounts.repository.AccountProfileLookup"
    );

    /**
     * Extrai todos os imports de um arquivo Java.
     */
    private static Set<String> extractImports(String filePath) throws IOException {
        Set<String> imports = new HashSet<>();
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            lines.filter(l -> l.trim().startsWith("import "))
                    .map(line -> line.trim()
                            .substring("import ".length())
                            .replaceAll(";\\s*$", "")
                            .trim())
                    .forEach(imports::add);
        }
        return imports;
    }

    /**
     * Verifica se um import pertence a um pacote base.
     */
    private static boolean importsFromPackage(String fullImport, String pkgPrefix) {
        return fullImport.startsWith(pkgPrefix + ".") || fullImport.equals(pkgPrefix);
    }

    /**
     * Regra 1: accounts não importa de auth exceto via allowlist.
     */
    @Test
    void accountsNaoDeveImportarDeAuthExcetoAllowlist() throws Exception {
        Path srcDir = Paths.get("src/main/java");
        Set<String> violations = new HashSet<>();

        try (Stream<Path> paths = Files.walk(srcDir)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().startsWith(srcDir.toAbsolutePath().toString() + "/dev/zayt/atrilha/accounts/"))
                    .forEach(path -> {
                        try {
                            Set<String> imports = extractImports(path.toString());
                            for (String imp : imports) {
                                if (importsFromPackage(imp, AUTH_PKG) && !ACCOUNTS_TO_AUTH_ALLOWLIST.contains(imp)) {
                                    String className = path.getFileName().toString().replace(".java", "");
                                    violations.add(className + " importa " + imp);
                                }
                            }
                        } catch (IOException e) {
                            // ignorar arquivos que não existem durante teste
                        }
                    });
        }

        if (!violations.isEmpty()) {
            fail("accounts importa de auth fora da allowlist:\n" + String.join("\n", violations));
        }
    }

    /**
     * Regra 2: auth não importa de accounts exceto via allowlist.
     */
    @Test
    void authNaoDeveImportarDeAccountsExcetoAllowlist() throws Exception {
        Path srcDir = Paths.get("src/main/java");
        Set<String> violations = new HashSet<>();

        try (Stream<Path> paths = Files.walk(srcDir)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().startsWith(srcDir.toAbsolutePath().toString() + "/dev/zayt/atrilha/auth/"))
                    .forEach(path -> {
                        try {
                            Set<String> imports = extractImports(path.toString());
                            for (String imp : imports) {
                                if (importsFromPackage(imp, ACCOUNTS_PKG) && !AUTH_TO_ACCOUNTS_ALLOWLIST.contains(imp)) {
                                    String className = path.getFileName().toString().replace(".java", "");
                                    violations.add(className + " importa " + imp);
                                }
                            }
                        } catch (IOException e) {
                            // ignorar
                        }
                    });
        }

        if (!violations.isEmpty()) {
            fail("auth importa de accounts fora da allowlist:\n" + String.join("\n", violations));
        }
    }

    /**
     * Regra 3: nenhum pacote *.web é importado por *.service ou *.domain.
     * Regra de fluxo: camadas superiores não devem importar camadas inferiores.
     */
    @Test
    void serviceENaoDevemImportarWeb() throws Exception {
        Path srcDir = Paths.get("src/main/java");
        Set<String> violations = new HashSet<>();

        try (Stream<Path> paths = Files.walk(srcDir)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        String abs = p.toAbsolutePath().toString();
                        return abs.contains("/service/") || abs.contains("/domain/");
                    })
                    .forEach(path -> {
                        try {
                            Set<String> imports = extractImports(path.toString());
                            for (String imp : imports) {
                                // Spring web é framework — não conta como camada web do projeto
                                if (imp.startsWith("org.springframework.web")) continue;
                                if (imp.contains(".web.")) {
                                    String className = path.getFileName().toString().replace(".java", "");
                                    violations.add(className + " importa de web: " + imp);
                                }
                            }
                        } catch (IOException e) {
                            // ignorar
                        }
                    });
        }

        if (!violations.isEmpty()) {
            fail("service/domain importa de web (viola regra de fluxo):\n" + String.join("\n", violations));
        }
    }

    /**
     * Regra 4: todos os pacotes sob ROOT_PACKAGE existem fisicamente.
     */
    @Test
    void todosPacotesRaizExistem() throws IOException {
        Path srcDir = Paths.get("src/main/java");
        String[] expectedPackages = {
                "dev/zayt/atrilha",
                "dev/zayt/atrilha/accounts",
                "dev/zayt/atrilha/accounts/avatar",
                "dev/zayt/atrilha/accounts/repository",
                "dev/zayt/atrilha/auth",
                "dev/zayt/atrilha/auth/login",
                "dev/zayt/atrilha/auth/verification",
                "dev/zayt/atrilha/auth/web",
                "dev/zayt/atrilha/auth/config",
                "dev/zayt/atrilha/notifications",
                "dev/zayt/atrilha/content",
                "dev/zayt/atrilha/progress",
                "dev/zayt/atrilha/admin",
                "dev/zayt/atrilha/web"
        };

        assertAll("Pacotes raiz devem existir", () -> {
            for (String pkg : expectedPackages) {
                Path dir = srcDir.resolve(pkg);
                if (!Files.isDirectory(dir)) {
                    fail("Pacote não existe: " + pkg);
                }
            }
        });
    }
}
