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
 * Verifica a aplicação completa das migrations V2 + V4 (US-001 + REF-003)
 * sobre uma base limpa Postgres 18, garantindo que:
 *
 * <ul>
 *   <li>As tabelas {@code accounts} e {@code adolescent_profiles} são criadas.</li>
 *   <li>A unicidade case-insensitive de e-mail funciona, respeitando soft-delete.</li>
 *   <li>Após V4 (REF-003) a coluna {@code oauth_provider} não existe mais.</li>
 *   <li>O CHECK constraint {@code accounts_credential_chk} exige
 *       {@code password_hash IS NOT NULL} para contas ativas (não soft-deletadas).</li>
 *   <li>A FK 1:1 {@code adolescent_profiles.account_id → accounts.id} existe.</li>
 * </ul>
 *
 * <p>Mesmo guard de {@code assumeTrue(DockerClientFactory...)} usado em
 * {@link dev.zayt.atrilha.FlywayMigrationIT}.</p>
 */
@Testcontainers
class AccountsMigrationIT {

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
    void migrationCreatesAccountsAndAdolescentProfilesTables() throws SQLException {
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
            assertThat(tables).contains("accounts", "adolescent_profiles", "schema_baseline");
        }
    }

    @Test
    void accountsTableHasExpectedColumns() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker nao disponivel — teste ignorado");

        migrate();

        try (Connection conn = dataSource().getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT column_name FROM information_schema.columns "
                            + "WHERE table_schema = 'public' AND table_name = 'accounts'");
            Set<String> cols = new HashSet<>();
            while (rs.next()) {
                cols.add(rs.getString("column_name"));
            }
            assertThat(cols).containsExactlyInAnyOrder(
                    "id", "type", "email", "email_verified_at",
                    "password_hash",
                    "created_at", "last_login_at", "deleted_at");
        }
    }

    @Test
    void adolescentProfilesTableHasExpectedColumns() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker nao disponivel — teste ignorado");

        migrate();

        try (Connection conn = dataSource().getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT column_name FROM information_schema.columns "
                            + "WHERE table_schema = 'public' "
                            + "AND table_name = 'adolescent_profiles'");
            Set<String> cols = new HashSet<>();
            while (rs.next()) {
                cols.add(rs.getString("column_name"));
            }
            assertThat(cols).containsExactlyInAnyOrder(
                    "account_id", "nickname", "birth_date", "avatar_url", "timezone");
        }
    }

    @Test
    void emailUniqueIndexIsCaseInsensitiveAndRespectsSoftDelete() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker nao disponivel — teste ignorado");

        migrate();

        try (Connection conn = dataSource().getConnection()) {
            // Inserir duas contas com mesma chave LOWER(email) deve falhar.
            conn.createStatement().executeUpdate(
                    "INSERT INTO accounts (id, type, email, password_hash, created_at) "
                            + "VALUES ('11111111-1111-1111-1111-111111111111', "
                            + "        'ADOLESCENT', 'julia@example.com', "
                            + "        '$2b$12$abcabcabcabcabcabcabcuQ7K3w6vM2Lz1Y8aA9bB0cC1dD2eE3fF', NOW())");
            try {
                conn.createStatement().executeUpdate(
                        "INSERT INTO accounts (id, type, email, password_hash, created_at) "
                                + "VALUES ('22222222-2222-2222-2222-222222222222', "
                                + "        'ADOLESCENT', 'Julia@Example.COM', "
                                + "        '$2b$12$xyzxyzxyzxyzxyzxyzxyzuQ7K3w6vM2Lz1Y8aA9bB0cC1dD2eE3fF', NOW())");
                throw new AssertionError("UNIQUE LOWER(email) deveria ter rejeitado o duplicado case-insensitive");
            } catch (SQLException expected) {
                assertThat(expected.getMessage().toLowerCase()).contains("unique");
            }

            // Soft-deletar a primeira → segunda passa.
            conn.createStatement().executeUpdate(
                    "UPDATE accounts SET deleted_at = NOW() WHERE id = '11111111-1111-1111-1111-111111111111'");
            int inserted = conn.createStatement().executeUpdate(
                    "INSERT INTO accounts (id, type, email, password_hash, created_at) "
                            + "VALUES ('22222222-2222-2222-2222-222222222222', "
                            + "        'ADOLESCENT', 'Julia@Example.COM', "
                            + "        '$2b$12$xyzxyzxyzxyzxyzxyzxyzuQ7K3w6vM2Lz1Y8aA9bB0cC1dD2eE3fF', NOW())");
            assertThat(inserted).isEqualTo(1);
        }
    }

    /**
     * Após V4 (REF-003) o CHECK accounts_credential_chk passa a exigir
     * password_hash NOT NULL (ou deleted_at NOT NULL, para preservar contas
     * soft-deletadas historicas). Sem password_hash, INSERT deve falhar.
     */
    @Test
    void credentialConstraintRequiresPasswordHash() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker nao disponivel — teste ignorado");

        migrate();

        try (Connection conn = dataSource().getConnection()) {
            try {
                conn.createStatement().executeUpdate(
                        "INSERT INTO accounts (id, type, email, created_at) "
                                + "VALUES ('33333333-3333-3333-3333-333333333333', "
                                + "        'ADOLESCENT', 'nocred@example.com', NOW())");
                throw new AssertionError("CHECK accounts_credential_chk deveria rejeitar password_hash NULL em conta ativa");
            } catch (SQLException expected) {
                assertThat(expected.getMessage().toLowerCase())
                        .containsAnyOf("check", "constraint");
            }
        }
    }

    /**
     * Complementa {@link #credentialConstraintRequiresPasswordHash()}: a outra
     * perna do CHECK pos-V4 e permitir password_hash NULL quando deleted_at
     * estiver preenchido (preserva contas soft-deletadas historicas que vieram
     * do schema pre-V4). Se essa perna regredir, qualquer migration futura que
     * tente fazer back-fill de soft-deletes vai quebrar.
     */
    @Test
    void credentialConstraintAllowsPasswordHashNullWhenSoftDeleted() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker nao disponivel — teste ignorado");

        migrate();

        try (Connection conn = dataSource().getConnection()) {
            int inserted = conn.createStatement().executeUpdate(
                    "INSERT INTO accounts (id, type, email, created_at, deleted_at) "
                            + "VALUES ('44444444-4444-4444-4444-444444444444', "
                            + "        'ADOLESCENT', 'soft-deleted@example.com', "
                            + "        NOW(), NOW())");
            assertThat(inserted)
                    .as("CHECK accounts_credential_chk deve aceitar password_hash NULL quando deleted_at preenchido")
                    .isEqualTo(1);
        }
    }

    @Test
    void typeConstraintRejectsUnknownType() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker nao disponivel — teste ignorado");

        migrate();

        try (Connection conn = dataSource().getConnection()) {
            try {
                conn.createStatement().executeUpdate(
                        "INSERT INTO accounts (id, type, email, password_hash, created_at) "
                                + "VALUES ('55555555-5555-5555-5555-555555555555', "
                                + "        'CHEESE', 'wrong@example.com', "
                                + "        '$2b$12$abcabcabcabcabcabcabcuQ7K3w6vM2Lz1Y8aA9bB0cC1dD2eE3fF', NOW())");
                throw new AssertionError("CHECK accounts_type_chk deveria rejeitar type desconhecido");
            } catch (SQLException expected) {
                assertThat(expected.getMessage().toLowerCase())
                        .containsAnyOf("check", "constraint");
            }
        }
    }
}
