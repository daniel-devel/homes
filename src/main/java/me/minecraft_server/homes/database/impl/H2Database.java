package me.minecraft_server.homes.database.impl;

import me.minecraft_server.homes.database.ISQLConfigurator;
import org.jetbrains.annotations.NotNull;

/**
 * Used to create a connection to a h2 database. Intended to be used in tests or as a file database.
 */
public final class H2Database {

    /**
     * Disable construction of this class.
     */
    private H2Database() { }

    /**
     * Create a memory database.
     * NOTE: This will always cause a memory leak, if the database is not properly closed.
     *       This is intended to only be used in tests.
     * @param pName The name of the target database.
     * @return A database configurator for that database.
     */
    public static @NotNull ISQLConfigurator memory(@NotNull final String pName) {
        return config -> {
            config.setDriverClassName("org.h2.Driver");
            config.setJdbcUrl("jdbc:h2:mem:" + pName + ";DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_LOWER=TRUE");
        };
    }

}
