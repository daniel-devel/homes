package me.minecraft_server.homes.database;

import com.google.common.collect.BiMap;
import me.minecraft_server.homes.dto.HomeLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IDatabase {

    /**
     * Gets a home of a player
     * @param pPlayer The player whose home it is.
     * @param pName The name of the home.
     * @return A location of the home, can be null if the home does not exist.
     */
    @Nullable HomeLocation getHome(@NotNull final UUID pPlayer, @NotNull final String pName);

    /**
     * Gets a home of a player
     * @param pHomeId The id of the home.
     * @return A location of the home, can be null if the home does not exist.
     */
    @Nullable HomeLocation getHome(int pHomeId);

    /**
     * Gets all homes of a player with their names.
     * @param pOwner The player whose homes to retrieve.
     * @return A map of players homes mapped by name to location. Can be empty if the player has no homes.
     */
    @NotNull Map<String, HomeLocation> getHomes(@NotNull final UUID pOwner);

    /**
     * Gets all homes ids of a player bi-mapped to their names.
     * @param pOwner The player whose homes to retrieve.
     * @return A map of players homes bi-mapped by home-id to name. Can be empty if the player has no homes.
     */
    @NotNull BiMap<@NotNull Integer, @NotNull String> getPlayerMappedHomes(@NotNull UUID pOwner);

    /**
     * Inserts a new home location. Fails if the home already exists.
     * @param pOwner The player who the home belongs to.
     * @param pName The name of the home.
     * @param pLocation The new location.
     * @return A boolean, whether the action was successful. Can be false, if the player already has a home with this name.
     */
    boolean addHome(@NotNull final UUID pOwner, @NotNull final String pName, @NotNull final HomeLocation pLocation);

    /**
     * Inserts or updates a new home location.
     * @param pOwner    The player who the home belongs to.
     * @param pName     The name of the home.
     * @param pLocation The new location.
     * @return A boolean, whether the action was successful.
     */
    @SuppressWarnings("UnusedReturnValue")
    boolean setHome(@NotNull final UUID pOwner, @NotNull final String pName, @NotNull final HomeLocation pLocation);

    /**
     * Updates the location of the home.
     * @param pHomeId   The id of the home.
     * @param pLocation The new location.
     * @return A boolean, whether the action was successful.
     */
    boolean updateHome(int pHomeId, @NotNull HomeLocation pLocation);

    /**
     * Deletes a home by its owner and name.
     * @param pOwner The player who the home belongs to.
     * @param pName  The name of the home.
     * @return A boolean, whether the action was successful.
     */
    boolean deleteHome(@NotNull UUID pOwner, @NotNull String pName);

    /**
     * Deletes a home by its id.
     * @param pHomeId The id of the home.
     * @return A boolean, whether the action was successful.
     */
    boolean deleteHome(int pHomeId);

    /**
     * Registers a player.
     * @param pOwner    The players uniqueId.
     * @param pUsername The players name.
     */
    @SuppressWarnings("UnusedReturnValue")
    void registerPlayer(@NotNull UUID pOwner, @NotNull String pUsername);

    /**
     * Returns a player name for a given unique id.
     * @param pUniqueId The unique id in question.
     * @return A future to a player name.
     */
    @Nullable String getPlayerName(@NotNull UUID pUniqueId);

    /**
     * Returns unique ids with a given username.
     * @param pUsername The username in question.
     * @return A future to a list of unique ids with that username.
     */
    @NotNull List<UUID> getPlayerUniqueId(@NotNull String pUsername);

}
