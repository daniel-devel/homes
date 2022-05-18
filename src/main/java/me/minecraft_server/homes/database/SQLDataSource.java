package me.minecraft_server.homes.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;

public class SQLDataSource implements Closeable {

    private @NotNull final HikariDataSource dataSource;

    public SQLDataSource(@NotNull final ISQLConfigurator configurator) {

        // Configure config
        final var config = new HikariConfig();
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        configurator.configure(config);

        // Create data source
        this.dataSource = new HikariDataSource(config);

    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
