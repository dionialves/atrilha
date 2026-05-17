package dev.zayt.atrilha;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class FlywayMigrationIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("atrilha")
                    .withUsername("atrilha")
                    .withPassword("atrilha");

    @Test
    void migrationAppliesBaselineAndRecordExists() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker nao disponivel — teste ignorado");

        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(POSTGRES.getJdbcUrl());
        ds.setUser(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());

        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load();

        flyway.migrate();

        try (Connection conn = ds.getConnection()) {
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT description FROM schema_baseline WHERE id = 1");
            rs.next();
            assertEquals("atrilha baseline schema", rs.getString("description"));
        }
    }
}
