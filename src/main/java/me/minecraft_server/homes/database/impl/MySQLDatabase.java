package me.minecraft_server.homes.database.impl;

import lombok.experimental.UtilityClass;
import me.minecraft_server.homes.database.ISQLConfigurator;
import org.jetbrains.annotations.NotNull;

/**
 * Used to create a connection to a MySQL database.
 */
@UtilityClass
public final class MySQLDatabase {

    /**
     * Creates a configuration to connect to the given mysql database with the provided settings.
     * @param pHost The host of the mysql database.
     * @param pPort The port of the mysql database, usually 3306.
     * @param pDatabase The database to connect to.
     * @param pUsername The username to use to connect to the database.
     * @param pPassword The password to use to connect to the database.
     * @return A database configurator to connect to that database.
     */
    public static @NotNull ISQLConfigurator connection(@NotNull final String pHost, final int pPort, @NotNull final String pDatabase, @NotNull final String pUsername, @NotNull final String pPassword) {
        return config -> {
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl("jdbc:mysql://" + pHost + ":" + pPort + "/" + pDatabase);
            config.setUsername(pUsername);
            config.setPassword(pPassword);
        };
    }

}
