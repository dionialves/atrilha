package dev.zayt.atrilha.accounts;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contratos estruturais do cadastro de adolescente (US-001):
 *
 * <ul>
 *   <li>Form renderizado tem token CSRF acessível ao Thymeleaf (proteção
 *       contra CSRF não pode regredir silenciosamente).</li>
 *   <li>Form aponta ({@code action}) e usa o método ({@code post}) que o
 *       controller declara — quebrar isso torna o submit dead-letter.</li>
 *   <li>Form é {@code multipart/form-data} para suportar upload de foto.</li>
 *   <li>Unicidade de e-mail é enforced pelo banco mesmo em INSERTs concorrentes
 *       diretos, simulando race condition entre dois cadastros simultâneos —
 *       a constraint do banco é a última linha de defesa (PRD §11 / LGPD).</li>
 * </ul>
 *
 * <p>Inspeção do HTML usa Jsoup sobre o DOM (não regex em template cru).</p>
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.baseline-on-migrate=false"
})
@ActiveProfiles("test")
@DirtiesContext
class RegistrationContractIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("atrilha")
                    .withUsername("atrilha")
                    .withPassword("atrilha");

    @DynamicPropertySource
    static void registerPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    WebApplicationContext ctx;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    // ============================================================
    // Contrato estrutural do form HTML — via Jsoup no DOM
    // ============================================================

    @Test
    void registrationFormContainsCsrfTokenAndCorrectActionAndEnctype() throws Exception {
        String html = mvc.perform(get("/cadastro/adolescente"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Document doc = Jsoup.parse(html);
        Element form = doc.selectFirst("form");
        assertThat(form)
                .as("/cadastro/adolescente deve renderizar um <form>")
                .isNotNull();

        // action="/cadastro/adolescente" (Thymeleaf @{...})
        assertThat(form.attr("action"))
                .as("form.action deve apontar para o endpoint do controller")
                .isEqualTo("/cadastro/adolescente");

        // method POST — Thymeleaf às vezes serializa em minúsculas
        assertThat(form.attr("method").toLowerCase())
                .as("form.method deve ser POST")
                .isEqualTo("post");

        // multipart/form-data — necessário para upload de foto
        assertThat(form.attr("enctype"))
                .as("form deve aceitar multipart para upload de foto opcional")
                .isEqualTo("multipart/form-data");

        // Token CSRF presente — Thymeleaf injeta <input type="hidden" name="_csrf">
        // automaticamente quando Spring Security está habilitado.
        Element csrfInput = form.selectFirst("input[type=hidden][name=_csrf]");
        assertThat(csrfInput)
                .as("form deve conter o token CSRF (hidden input name=_csrf)")
                .isNotNull();
        assertThat(csrfInput.attr("value"))
                .as("token CSRF deve ter valor não-vazio")
                .isNotBlank();
    }

    /**
     * Inputs precisam ter o atributo {@code name} esperado pelo controller
     * (que faz binding por {@code @Valid RegisterAdolescentForm}). Se algum
     * apelido (atributo {@code name}) regredir — ex.: "nick" em vez de
     * "nickname" — o binding cai silenciosamente para null e o teste do
     * happy path do controller ainda passaria (a validação NotBlank pega).
     * Este teste é o guardrail explícito do contrato.
     */
    @Test
    void registrationFormHasAllRequiredInputNames() throws Exception {
        String html = mvc.perform(get("/cadastro/adolescente"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Document doc = Jsoup.parse(html);

        assertThat(doc.selectFirst("input[name=email]"))
                .as("input name=email obrigatório").isNotNull();
        assertThat(doc.selectFirst("input[name=password]"))
                .as("input name=password obrigatório").isNotNull();
        assertThat(doc.selectFirst("input[name=nickname]"))
                .as("input name=nickname obrigatório").isNotNull();
        assertThat(doc.selectFirst("input[name=birthDate]"))
                .as("input name=birthDate obrigatório (camelCase é o binding name)").isNotNull();
        assertThat(doc.selectFirst("input[name=photo]"))
                .as("input name=photo obrigatório").isNotNull();
    }

    // ============================================================
    // Race-condition de e-mail duplicado — constraint do banco vence
    // ============================================================

    /**
     * Duas threads tentam INSERT simultâneo com o mesmo e-mail (variações
     * de case). Apenas uma deve persistir; a outra cai na constraint do
     * índice único {@code accounts_email_unique} (LOWER(email)).
     *
     * <p>Diferente do teste do controller (que serializa requisições na
     * mesma thread), aqui validamos o caso real de race condition: dois
     * cadastros simultâneos NO BANCO produzem exatamente uma conta — não
     * duas com mesmo LOWER(email).</p>
     */
    @Test
    void concurrentInsertsWithSameEmailCaseInsensitiveResultInExactlyOneRow() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker nao disponivel — teste ignorado");

        // Setup: aplica migrations e usa um banco dedicado pra não colidir
        // com o estado dos outros ITs (que já criaram contas em paralelo).
        DataSource ds = dataSourceForOwnSchema();
        applyMigrations(ds);

        String email = "race@example.com";
        String emailUpper = "RACE@example.com";
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch starter = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        Future<?> f1 = pool.submit(() ->
                attemptInsert(ds, "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1", email, starter, successes, failures));
        Future<?> f2 = pool.submit(() ->
                attemptInsert(ds, "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2", emailUpper, starter, successes, failures));

        starter.countDown(); // libera as duas threads juntas
        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(successes.get())
                .as("exatamente uma das inserts concorrentes deve persistir")
                .isEqualTo(1);
        assertThat(failures.get())
                .as("a outra insert deve falhar na constraint UNIQUE")
                .isEqualTo(1);

        // Sanity: o índice único de fato impediu duplicata.
        try (Connection conn = ds.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM accounts WHERE LOWER(email) = LOWER('" + email + "')");
            rs.next();
            assertThat(rs.getInt(1))
                    .as("apenas uma conta para LOWER(email)")
                    .isEqualTo(1);
        }
    }

    private void attemptInsert(DataSource ds, String id, String email,
                                CountDownLatch starter,
                                AtomicInteger successes, AtomicInteger failures) {
        try {
            starter.await();
            try (Connection conn = ds.getConnection()) {
                conn.createStatement().executeUpdate(
                        "INSERT INTO accounts (id, type, email, password_hash, created_at) "
                                + "VALUES ('" + id + "', 'ADOLESCENT', '" + email + "', "
                                + "'$2b$12$" + "x".repeat(53) + "', NOW())");
                successes.incrementAndGet();
            }
        } catch (SQLException e) {
            failures.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private DataSource dataSourceForOwnSchema() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(POSTGRES.getJdbcUrl());
        ds.setUser(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());
        return ds;
    }

    private void applyMigrations(DataSource ds) {
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }
}
