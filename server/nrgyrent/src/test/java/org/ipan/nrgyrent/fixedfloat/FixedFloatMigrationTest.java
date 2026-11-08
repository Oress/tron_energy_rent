package org.ipan.nrgyrent.fixedfloat;

import liquibase.Liquibase;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class FixedFloatMigrationTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void migratesLegacyRowsAndPreservesExistingProviderChoice(boolean existingChoice) throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:ffmigration;MODE=PostgreSQL;NON_KEYWORDS=VALUE", "sa", "")) {
            var statement = connection.createStatement();
            statement.execute("create table nrg_deposit_transactions (id bigint primary key, status varchar(255))");
            statement.execute("create table nrg_configs (id varchar(255) primary key, value varchar(255), updated_at timestamp)");
            if (existingChoice) {
                statement.execute("insert into nrg_configs (id, value) values ('USDT_EXCHANGE_PROVIDER', 'FIXEDFLOAT')");
            }
            statement.execute("insert into nrg_deposit_transactions values (1, 'USDT_ENERGY_RENTED')");
            try (var liquibase = new Liquibase("db/changelog/changelog-1.2.xml",
                    new ClassLoaderResourceAccessor(), new JdbcConnection(connection))) {
                liquibase.update(new Contexts(), new LabelExpression());
                var setting = statement.executeQuery("select value from nrg_configs where id='USDT_EXCHANGE_PROVIDER'");
                assertTrue(setting.next());
                assertEquals(existingChoice ? "FIXEDFLOAT" : "BYBIT", setting.getString(1));
                assertFalse(setting.next());
                var rows = statement.executeQuery("select exchange_provider, exchange_order_id from nrg_deposit_transactions where id=1");
                assertTrue(rows.next());
                assertNull(rows.getString(1));
                assertNull(rows.getString(2));
                statement.execute("insert into nrg_deposit_transactions (id, exchange_provider, exchange_order_id, exchange_trx_tx) values (2, 'FIXEDFLOAT', 'ABC123', 'trx-tx')");
                assertThrows(SQLException.class, () -> statement.execute("insert into nrg_deposit_transactions (id, exchange_provider, exchange_order_id) values (3, 'FIXEDFLOAT', 'ABC123')"));
                assertThrows(SQLException.class, () -> statement.execute("insert into nrg_deposit_transactions (id, exchange_trx_tx) values (4, 'trx-tx')"));
                statement.execute("insert into nrg_ff_review_notifications values (2, 99, CURRENT_TIMESTAMP)");
                assertThrows(SQLException.class, () -> statement.execute("insert into nrg_ff_review_notifications values (2, 99, CURRENT_TIMESTAMP)"));
                statement.execute("insert into nrg_ff_review_notifications values (2, 100, CURRENT_TIMESTAMP)");
                assertThrows(SQLException.class, () -> statement.execute("insert into nrg_ff_review_notifications values (999, 99, CURRENT_TIMESTAMP)"));
            }
        }
    }
}
