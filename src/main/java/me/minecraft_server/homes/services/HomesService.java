package me.minecraft_server.homes.services;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import lombok.Getter;
import me.minecraft_server.homes.Homes;
import me.minecraft_server.homes.database.SQLDataSource;
import me.minecraft_server.homes.database.SQLDatabase;
import me.minecraft_server.homes.database.impl.MySQLDatabase;
import me.minecraft_server.homes.dto.HomeLocation;
import me.minecraft_server.homes.exceptions.HomeNotFoundException;
import me.minecraft_server.homes.exceptions.NotUniquelyIdentifiableException;
import me.minecraft_server.homes.exceptions.RegisteredPlayerNotFoundException;
import me.minecraft_server.homes.util.CommandUtils;
import me.minecraft_server.homes.services.homes.HomeTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

public final class HomesService implements Listener {

    @NotNull
    private final SQLDataSource source;

    @NotNull
    private final SQLDatabase database;

    @Getter
    @NotNull
    private final String server;

    @Getter
    @NotNull
    private final String defaultHome;

    @NotNull
    private final Homes plugin;

    public HomesService(@NotNull final Homes pPlugin) {
        this.plugin = pPlugin;

        // Get config
        final var config = pPlugin.getConfig();

        // Connect to the database
        final var host = config.getString("database.host", "localhost");
        final var port = config.getInt("database.port", 3306);
        final var database = config.getString("database.database", "homes");
        final var username = config.getString("database.username", "username");
        final var password = config.getString("database.password", "password");
        this.source = new SQLDataSource(MySQLDatabase.connection(host, port, database, username, password));
        this.database = new SQLDatabase(this.source);

        // Configure other properties
        this.server = config.getString("server", "");
        this.defaultHome = config.getString("default_home", "default");

    }

    /**
     * This cache contains homes identified by id.
     */
    @NotNull
    private final LoadingCache<Integer, HomeLocation> cachedLocations = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5L))
            .build(new CacheLoader<>() {
                @Override
                public @NotNull HomeLocation load(@NotNull final Integer pHomeId) throws HomeNotFoundException {
                    final var home = database.getHome(pHomeId);
                    if (home == null)
                        throw new HomeNotFoundException(new HomeTarget.Identifier(pHomeId));
                    return home;
                }
            });

    /**
     * This cache contains id and name of all homes of a player.
     * The id can then be used in the locations cache.
     */
    @NotNull
    private final LoadingCache<UUID, BiMap<Integer, String>> cachedPlayerHomes = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5L))
            .build(new CacheLoader<>() {
                @Override
                public @NotNull BiMap<Integer, String> load(@NotNull UUID pOwner) {
                    return database.getPlayerMappedHomes(pOwner);
                }
            });

    /**
     * This cache contains unique ids by player names.
     * Used to translate player names to unique ids.
     */
    @NotNull
    private final LoadingCache<String, List<UUID>> cachedPlayerNames = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5L))
            .build(new CacheLoader<>() {
                @Override
                public @NotNull List<UUID> load(@NotNull String pUsername) {
                    return database.getPlayerUniqueId(pUsername);
                }
            });

    /**
     * Gets the home location for a target.
     * @param pHomeTarget The home target.
     * @param pInitiator The initiator, required for own home targets.
     * @return The home location.
     * @throws HomeNotFoundException Thrown if the home can't be found. Either because it does not exist or
     *                               because of other exceptions occurring at retrieving the data.
     * @throws NotUniquelyIdentifiableException Thrown if the owner can't be uniquely identified, usually because
     *                                          multiple unique ids are registered with the same username.
     * @throws RegisteredPlayerNotFoundException Thrown if no unique ids can be retrieved for a username.
     * @throws UnsupportedOperationException Thrown if some implementation is missing.
     */
    public @NotNull HomeLocation getHomeLocation(@NotNull HomeTarget pHomeTarget, @Nullable Player pInitiator) throws HomeNotFoundException, NotUniquelyIdentifiableException, RegisteredPlayerNotFoundException, UnsupportedOperationException {
        try {
            return cachedLocations.get(getHomeId(pHomeTarget, pInitiator));
        } catch (NotUniquelyIdentifiableException | HomeNotFoundException | RegisteredPlayerNotFoundException | UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            if (e.getCause() instanceof HomeNotFoundException)
                throw (HomeNotFoundException) e.getCause();
            throw new HomeNotFoundException(pHomeTarget, e);
        }
    }

    /**
     * Gets the home id for a target.
     * @param pHomeTarget The home target.
     * @param pInitiator The initiator, required for own home targets.
     * @return The home location.
     * @throws HomeNotFoundException Thrown if the home can't be found. Either because it does not exist or
     *                               because of other exceptions occurring at retrieving the data.
     * @throws NotUniquelyIdentifiableException Thrown if the owner can't be uniquely identified, usually because
     *                                          multiple unique ids are registered with the same username.
     * @throws RegisteredPlayerNotFoundException Thrown if no unique ids can be retrieved for a username.
     * @throws UnsupportedOperationException Thrown if some implementation is missing.
     */
    public int getHomeId(@NotNull HomeTarget pHomeTarget, @Nullable Player pInitiator) throws HomeNotFoundException, NotUniquelyIdentifiableException, RegisteredPlayerNotFoundException, UnsupportedOperationException {
        try {
            switch (pHomeTarget) {

                case HomeTarget.Identifier identifier -> {
                    return identifier.value();
                }

                case HomeTarget.ForeignHomeName foreignName -> {
                    final List<UUID> playerUniqueIds = cachedPlayerNames.get(foreignName.owner());
                    if (playerUniqueIds.size() == 0)
                        throw new RegisteredPlayerNotFoundException(foreignName.owner());
                    else if (playerUniqueIds.size() > 1)
                        throw new NotUniquelyIdentifiableException(foreignName.owner());
                    final var homeId = cachedPlayerHomes.get(playerUniqueIds.get(0)).inverse().get(foreignName.name());
                    if (homeId == null)
                        throw new HomeNotFoundException(foreignName);
                    return homeId;
                }

                case HomeTarget.OwnHomeName ownName -> {
                    Preconditions.checkNotNull(pInitiator, "Can't identify a non-foreign home without an initiator.");
                    final var homeId = cachedPlayerHomes.get(pInitiator.getUniqueId()).inverse().get(ownName.name());
                    if (homeId == null)
                        throw new HomeNotFoundException(ownName);
                    return homeId;
                }

                case HomeTarget.ForeignHomeNameUnique uniqueForeignName -> {
                    final var homeId = cachedPlayerHomes.get(uniqueForeignName.owner()).inverse().get(uniqueForeignName.name());
                    if (homeId == null)
                        throw new HomeNotFoundException(uniqueForeignName);
                    return homeId;
                }

                default -> throw new UnsupportedOperationException("This is not implemented, yet.");

            }
        } catch (NotUniquelyIdentifiableException | HomeNotFoundException | RegisteredPlayerNotFoundException | UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            if (e.getCause() instanceof HomeNotFoundException)
                throw (HomeNotFoundException) e.getCause();
            throw new HomeNotFoundException(pHomeTarget, e);
        }
    }

    /**
     * Helper function for {@link #setHomeLocation(HomeTarget, Player, boolean)} to set home and invalidate for on home name targets.
     * @param pOwner The owner of the home.
     * @param pName The name of the home.
     * @param pLocation The location to set.
     * @param override Allow updating existing homes or just adding new ones?
     * @return Whether the action was successful.
     */
    private boolean setHomeLocationAndInvalidate(@NotNull final UUID pOwner, @NotNull final String pName, @NotNull final HomeLocation pLocation, final boolean override) {
        final var result = override ? database.setHome(pOwner, pName, pLocation)
                : database.addHome(pOwner, pName, pLocation);
        if (result) {
            try {
                cachedPlayerHomes.invalidate(pOwner);
                final var map = cachedPlayerHomes.get(pOwner);
                final var id = map.inverse().get(pName);
                if (id != null)
                    cachedLocations.invalidate(id);
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * @param pHomeTarget The home to set.
     * @param pSelf The player who is adding the location.
     * @param pOverride If it should override existing locations.
     *                 Note: This option is ignored on identifiers since you can't add them, only modify.
     * @return A future to boolean, whether the execution was successful.
     * @throws NotUniquelyIdentifiableException Thrown if the owner can't be uniquely identified, usually because
     *                                          multiple unique ids are registered with the same username.
     * @throws RegisteredPlayerNotFoundException Thrown if no unique ids can be retrieved for a username.
     */
    public boolean setHomeLocation(@NotNull HomeTarget pHomeTarget, @NotNull Player pSelf, boolean pOverride) throws NotUniquelyIdentifiableException, RegisteredPlayerNotFoundException {
        final var location = new HomeLocation(pSelf.getLocation(), server);
        switch (pHomeTarget) {

            case HomeTarget.Identifier identifier -> {
                final var result = database.updateHome(identifier.value(), location);
                if (result)
                    cachedLocations.invalidate(identifier.value());
                return result;
            }

            case HomeTarget.OwnHomeName ownName -> {
                return setHomeLocationAndInvalidate(pSelf.getUniqueId(), ownName.name(), location, pOverride);
            }

            case HomeTarget.ForeignHomeNameUnique uniqueForeignName -> {
                return setHomeLocationAndInvalidate(uniqueForeignName.owner(), uniqueForeignName.name(), location, pOverride);
            }

            case HomeTarget.ForeignHomeName foreignName -> {
                try {
                    final var playerUniqueIds = cachedPlayerNames.get(foreignName.owner());
                    if (playerUniqueIds.size() == 0)
                        throw new RegisteredPlayerNotFoundException(foreignName.owner());
                    else if (playerUniqueIds.size() > 1)
                        throw new NotUniquelyIdentifiableException(foreignName.owner());
                    return setHomeLocationAndInvalidate(playerUniqueIds.get(0), foreignName.name(), location, pOverride);
                } catch (ExecutionException e) {
                    throw new RegisteredPlayerNotFoundException(foreignName.owner(), e);
                }
            }

            default -> throw new UnsupportedOperationException("Not implemented, yet.");

        }
    }

    /**
     * A helper for {@link #deleteHomeLocation(HomeTarget, Player)} to delete and invalidate home names.
     * @param pOwner The owner of the home.
     * @param pName The name of the home.
     * @return Whether the action was successful.
     */
    private boolean deleteHomeLocationAndInvalidate(@NotNull final UUID pOwner, @NotNull final String pName) {
        final var result = database.deleteHome(pOwner, pName);
        final var map = cachedPlayerHomes.getIfPresent(pOwner);
        if (map != null) {
            final var id = map.inverse().get(pName);
            if (id != null)
                cachedLocations.invalidate(id);
        }
        cachedPlayerHomes.invalidate(pOwner);
        return result;
    }

    /**
     * Deletes a home.
     * @param pHomeTarget The home to delete.
     * @param pSelf The player who is deleting the location.
     * @return Whether the action was successful.
     */
    public boolean deleteHomeLocation(@NotNull HomeTarget pHomeTarget, @NotNull Player pSelf) {
        switch (pHomeTarget) {

            case HomeTarget.Identifier identifier -> {
                final var result = database.deleteHome(identifier.value());
                cachedLocations.invalidate(identifier.value());
                return result;
            }

            case HomeTarget.OwnHomeName ownName -> {
                return deleteHomeLocationAndInvalidate(pSelf.getUniqueId(), ownName.name());
            }

            case HomeTarget.ForeignHomeNameUnique uniqueForeignName -> {
                return deleteHomeLocationAndInvalidate(uniqueForeignName.owner(), uniqueForeignName.name());
            }

            case HomeTarget.ForeignHomeName foreignName -> {
                try {
                    final var playerUniqueIds = cachedPlayerNames.get(foreignName.owner());
                    if (playerUniqueIds.size() == 0)
                        throw new RegisteredPlayerNotFoundException(foreignName.owner());
                    else if (playerUniqueIds.size() > 1)
                        throw new NotUniquelyIdentifiableException(foreignName.owner());
                    return deleteHomeLocationAndInvalidate(playerUniqueIds.get(0), foreignName.name());
                } catch (ExecutionException e) {
                    throw new RegisteredPlayerNotFoundException(foreignName.owner(), e);
                }
            }

            default -> throw new UnsupportedOperationException("Not implemented, yet");

        }
    }

    /**
     * Shutdowns all connections.
     */
    public void shutdown() {
        source.close();
    }

    /**
     * Invalidates the player.
     */
    @EventHandler
    private void OnPlayerQuit(PlayerQuitEvent e) {
        cachedPlayerHomes.invalidate(e.getPlayer().getUniqueId());
        cachedPlayerNames.invalidate(e.getPlayer().getName());
    }

    /**
     * Registers and invalidates the player.
     */
    @EventHandler
    private void OnPlayerJoin(PlayerJoinEvent e) {
        database.registerPlayer(e.getPlayer().getUniqueId(), e.getPlayer().getName());
        cachedPlayerHomes.invalidate(e.getPlayer().getUniqueId());
        cachedPlayerNames.invalidate(e.getPlayer().getName());
    }

    /**
     * Returns home names or an empty list if they are currently not loaded in.
     * If the homes are not loaded in, then it schedules a load asynchronously.
     * @param pOwner The player whose homes to get.
     * @return The list of homes.
     */
    public Iterable<String> getHomeNamesAsync(@NotNull UUID pOwner) {
        final var homes = cachedPlayerHomes.getIfPresent(pOwner);
        if (homes == null) {
            plugin.getAsyncExecutor().execute(() -> {
                try {
                    cachedPlayerHomes.get(pOwner);
                } catch (ExecutionException ignored) { }
            });
            return Collections.emptyList();
        }
        return homes.values();
    }

    /**
     * Returns home names or an empty list if they are currently not loaded in.
     * If the homes are not loaded in, then it schedules a load asynchronously.
     * @param pUsername The player whose homes to get.
     * @return The list of homes.
     */
    public Iterable<String> getHomeNamesAsync(@NotNull String pUsername) {

        UUID uniqueId = null;
        if (pUsername.length() <= 16) {
            final var playerUniqueIds = cachedPlayerNames.getIfPresent(pUsername);
            if (playerUniqueIds == null) {
                plugin.getAsyncExecutor().execute(() -> {
                    try {
                        final var list = cachedPlayerNames.get(pUsername);
                        if (list.size() == 1)
                            cachedPlayerHomes.get(list.get(0));
                    } catch (ExecutionException ignored) { }
                });
            } else if (playerUniqueIds.size() == 1) {
                uniqueId = playerUniqueIds.get(0);
            }
        } else try {
             uniqueId = UUID.fromString(pUsername);
        } catch (IllegalArgumentException ignored) { }

        if (uniqueId == null)
            return Collections.emptyList();

        final var homes = getHomeNamesAsync(uniqueId);
        final var possibles = new ArrayList<String>();
        for (final var home : homes)
            possibles.add(pUsername + ":" + home);
        return possibles;

    }

    /**
     * Retrieves possible tab completion for home targets.
     * @param pPermissible The person running the tab completion.
     * @param pArgument The argument to complete.
     * @param pForeignPermission The permission required for the person to be able to use foreign home targets.
     * @return A list of possible completions.
     */
    public List<String> getPossibleHomes(@NotNull final Permissible pPermissible, @NotNull final String pArgument, @NotNull final String pForeignPermission) {
        if (pPermissible.hasPermission(pForeignPermission)) {
            final var split = pArgument.split(":", 2);
            if (split.length == 2)
                return CommandUtils.getPossibleCompletion(pArgument, plugin.getHomesService().getHomeNamesAsync(split[0]));
            if (!(pPermissible instanceof Player))
                return null;
        }
        if (pPermissible instanceof Player)
            return CommandUtils.getPossibleCompletion(pArgument, plugin.getHomesService().getHomeNamesAsync(((Player) pPermissible).getUniqueId()));
        return Collections.emptyList();
    }

}
