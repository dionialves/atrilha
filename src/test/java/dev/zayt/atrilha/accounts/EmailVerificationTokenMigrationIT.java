package dev.zayt.atrilha.accounts;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Valida que a migration V3 (US-006) cria a tabela
 * {@code email_verification_token} com todas as colunas esperadas, o índice
 * em {@code token} e a FK para {@code accounts(id)}.
 *
 * <p>Não recria a tabela {@code accounts} — depende da V2 (US-001) já no
 * histórico do Flyway.</p>
 */
@Testcontainers
class EmailVerificationTokenMigrationIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("atrilha")
                    .withUsername("atrilha")
                    .withPassword("atrilha");

    private PGSimpleDataSource dataSource() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(POSTGRES.getJdbcUrl());
        ds.setUser(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());
        return ds;
    }

    private void migrate() {
        Flyway.configure()
                .dataSource(dataSource())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load()
                .migrate();
    }

    @Test
    void migrationCreatesEmailVerificationTokenTable() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker nao disponivel — teste ignorado");

        migrate();

        try (Connection conn = dataSource().getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT table_name FROM information_schema.tables "
                            + "WHERE table_schema = 'public' "
                            + "ORDER BY table_name");
            Set<String> tables = new HashSet<>();
            while (rs.next()) {
                tables.add(rs.getString("table_name"));
            }
            assertThat(tables).contains("email_verification_token");
        }
    }

    @Test
    void emailVerificationTokenTableHasExpectedColumns() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker nao disponivel — teste ignorado");

        migrate();

        try (Connection conn = dataSource().getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT column_name FROM information_schema.columns "
                            + "WHERE table_schema = 'public' AND table_name = 'email_verification_token'");
            Set<String> cols = new HashSet<>();
            while (rs.next()) {
                cols.add(rs.getString("column_name"));
            }
            assertThat(cols).containsExactlyInAnyOrder(
                    "id", "account_id", "token", "expires_at", "used_at", "created_at");
        }
    }

    @Test
    void tokenColumnHasUniqueIndex() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker nao disponivel — teste ignorado");

        migrate();

        try (Connection conn = dataSource().getConnection()) {
            // Inserir uma account válida para a FK
            conn.createStatement().executeUpdate(
                    "INSERT INTO accounts (id, type, email, password_hash, created_at) "
                            + "VALUES ('11111111-1111-1111-1111-111111111111', "
                            + "        'ADOLESCENT', 'a@b.com', "
                            + "        '$2b$12$" + "a".repeat(53) + "', NOW())");

            // Dois tokens com mesmo valor de token UUID — deve falhar (UNIQUE)
            String tokenUuid = "22222222-2222-2222-2222-222222222222";
            conn.createStatement().executeUpdate(
                    "INSERT INTO email_verification_token (id, account_id, token, expires_at) "
                            + "VALUES ('33333333-3333-3333-3333-333333333333', "
                            + "        '11111111-1111-1111-1111-111111111111', "
                            + "        '" + tokenUuid + "', NOW() + INTERVAL '24 hours')");
            try {
                conn.createStatement().executeUpdate(
                        "INSERT INTO email_verification_token (id, account_id, token, expires_at) "
                                + "VALUES ('44444444-4444-4444-4444-444444444444', "
                                + "        '11111111-1111-1111-1111-111111111111', "
                                + "        '" + tokenUuid + "', NOW() + INTERVAL '24 hours')");
                throw new AssertionError("UNIQUE em token deveria ter rejeitado o duplicado");
            } catch (SQLException expected) {
                assertThat(expected.getMessage().toLowerCase())
                        .containsAnyOf("unique", "duplicate");
            }
        }
    }

    @Test
    void accountFkCascadesDeletes() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker nao disponivel — teste ignorado");

        migrate();

        try (Connection conn = dataSource().getConnection()) {
            conn.createStatement().executeUpdate(
                    "INSERT INTO accounts (id, type, email, password_hash, created_at) "
                            + "VALUES ('55555555-5555-5555-5555-555555555555', "
                            + "        'ADOLESCENT', 'fk@b.com', "
                            + "        '$2b$12$" + "b".repeat(53) + "', NOW())");
            conn.createStatement().executeUpdate(
                    "INSERT INTO email_verification_token (id, account_id, token, expires_at) "
                            + "VALUES ('66666666-6666-6666-6666-666666666666', "
                            + "        '55555555-5555-5555-5555-555555555555', "
                            + "        '77777777-7777-7777-7777-777777777777', NOW() + INTERVAL '24 hours')");

            int deleted = conn.createStatement().executeUpdate(
                    "DELETE FROM accounts WHERE id = '55555555-5555-5555-5555-555555555555'");
            assertThat(deleted).isEqualTo(1);

            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM email_verification_token "
                            + "WHERE account_id = '55555555-5555-5555-5555-555555555555'");
            rs.next();
            assertThat(rs.getInt(1))
                    .as("ON DELETE CASCADE deve ter removido o token")
                    .isEqualTo(0);
        }
    }
}
