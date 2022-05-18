package me.minecraft_server.homes.database;

import com.zaxxer.hikari.HikariConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Configures a hikari config.
 */
@FunctionalInterface
public interface ISQLConfigurator {

    /**
     * Configures a hikari config, so it can create a database connection.
     * @param pConfig The config to configure.
     */
    void configure(@NotNull final HikariConfig pConfig);

}
