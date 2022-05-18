package me.minecraft_server.homes.database;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import me.minecraft_server.homes.dto.HomeLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.SQLException;
import java.util.*;

@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve"})
public class SQLDatabase implements IDatabase {

    @NotNull final SQLDataSource mSource;

    public SQLDatabase(@NotNull final SQLDataSource pSource) {
        this.mSource = pSource;
        createTables(); // Prepare database
    }

    /**
     * ! This is called at creation of the class !
     * Creates the required tables:
     *  Players(!uniqueId(B16), username(VC16))
     *  Homes(!homeId(Int), name(VC64), server(VC32), world(VC32), x(D), y(D), y(D), yaw(F), pitch(F), !#Players.uniqueId(B16))
     */
    private void createTables() {
        try (final var connection = mSource.getConnection();
             final var playersTable = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `Players` (`uniqueId` BINARY(16) NOT NULL, `username` VARCHAR(16) NOT NULL, PRIMARY KEY (`uniqueId`));");
             final var homesTables = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `Homes` (`homeId` INTEGER NOT NULL AUTO_INCREMENT, `name` VARCHAR(64) NOT NULL, `server` VARCHAR(32) NOT NULL, `world` VARCHAR(32) NOT NULL, `x` DOUBLE NOT NULL, `y` DOUBLE NOT NULL, `z` DOUBLE NOT NULL, `yaw` FLOAT NOT NULL, `pitch` FLOAT NOT NULL, `uniqueId` BINARY(16) NOT NULL, CONSTRAINT UC_HomeId UNIQUE (`homeId`), CONSTRAINT UC_OnlyOneName UNIQUE (`name`, `uniqueId`), PRIMARY KEY (`homeId`, `uniqueId`), CONSTRAINT FK_HomeOwner FOREIGN KEY (`uniqueId`) REFERENCES `Players`(`uniqueId`));")) {
            playersTable.executeUpdate();
            homesTables.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public @Nullable HomeLocation getHome(@NotNull final UUID pOwner, @NotNull final String pName) {
        try (final var connection = mSource.getConnection();
             final var statement = connection.prepareStatement(
                     "SELECT `server`, `world`, `x`, `y`, `z`, `yaw`, `pitch` FROM `Homes` WHERE `uniqueId` = ? AND `name` = ?;")) {
            statement.setBytes(1, toBytes(pOwner));
            statement.setString(2, pName);
            try (final var result = statement.executeQuery()) {
                if (result.next()) {
                    final var server = result.getString(1);
                    final var world = result.getString(2);
                    final var x = result.getDouble(3);
                    final var y = result.getDouble(4);
                    final var z = result.getDouble(5);
                    final var yaw = result.getFloat(6);
                    final var pitch = result.getFloat(7);
                    return new HomeLocation(x, y, z, yaw, pitch, world, server);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Some error occurred or the player has no home with that name.
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public @Nullable HomeLocation getHome(final int pHomeId) {
        try (final var connection = mSource.getConnection();
             final var statement = connection.prepareStatement(
                     "SELECT `server`, `world`, `x`, `y`, `z`, `yaw`, `pitch` FROM `Homes` WHERE `homeId` = ?;")) {
            statement.setInt(1, pHomeId);
            try (final var result = statement.executeQuery()) {
                if (result.next()) {
                    final var server = result.getString(1);
                    final var world = result.getString(2);
                    final var x = result.getDouble(3);
                    final var y = result.getDouble(4);
                    final var z = result.getDouble(5);
                    final var yaw = result.getFloat(6);
                    final var pitch = result.getFloat(7);
                    return new HomeLocation(x, y, z, yaw, pitch, world, server);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Some error occurred or the player has no home with that name.
    }

    @Override
    public @NotNull Map<String, HomeLocation> getHomes(@NotNull UUID pOwner) {
        try (final var connection = mSource.getConnection();
             final var statement = connection.prepareStatement(
                     "SELECT `name`, `server`, `world`, `x`, `y`, `z`, `yaw`, `pitch` FROM `Homes` WHERE `uniqueId` = ?;")) {
            statement.setBytes(1, toBytes(pOwner));
            try (final var result = statement.executeQuery()) {
                final var map = new HashMap<String, HomeLocation>();
                while (result.next()) {
                    final var name = result.getString(1);
                    final var server = result.getString(2);
                    final var world = result.getString(3);
                    final var x = result.getDouble(4);
                    final var y = result.getDouble(5);
                    final var z = result.getDouble(6);
                    final var yaw = result.getFloat(7);
                    final var pitch = result.getFloat(8);
                    map.put(name, new HomeLocation(x, y, z, yaw, pitch, world, server));
                }
                return map;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyMap(); // Some error occurred.
    }

    @Override
    public @NotNull BiMap<@NotNull Integer, @NotNull String> getPlayerMappedHomes(@NotNull UUID pOwner) {
        try (final var connection = mSource.getConnection();
             final var statement = connection.prepareStatement(
                     "SELECT `homeId`, `name` FROM `Homes` WHERE `uniqueId` = ?;")) {
            statement.setBytes(1, toBytes(pOwner));
            try (final var result = statement.executeQuery()) {
                final BiMap<Integer, String> map = HashBiMap.create();
                while (result.next())
                    map.put(result.getInt(1), result.getString(2));
                return map;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ImmutableBiMap.of(); // Some error occurred.
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public boolean addHome(@NotNull UUID pOwner, @NotNull String pName, @NotNull HomeLocation pLocation) {
        try (final var connection = mSource.getConnection();
             final var statement = connection.prepareStatement(
                     "INSERT IGNORE INTO `Homes` (`name`, `server`, `world`, `x`, `y`, `z`, `yaw`, `pitch`, `uniqueId`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
            statement.setString(1, pName);
            statement.setString(2, pLocation.getServer());
            statement.setString(3, pLocation.getWorld());
            statement.setDouble(4, pLocation.getX());
            statement.setDouble(5, pLocation.getY());
            statement.setDouble(6, pLocation.getZ());
            statement.setFloat(7, pLocation.getYaw());
            statement.setFloat(8, pLocation.getPitch());
            statement.setBytes(9, toBytes(pOwner));
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Some error occurred.
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public boolean setHome(@NotNull UUID pOwner, @NotNull String pName, @NotNull HomeLocation pLocation) {
        try (final var connection = mSource.getConnection();
             final var statement = connection.prepareStatement(
                     "INSERT INTO `Homes` (`name`, `server`, `world`, `x`, `y`, `z`, `yaw`, `pitch`, `uniqueId`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                             "ON DUPLICATE KEY UPDATE `server` = ?, `world` = ?, `x` = ?, `y` = ?, `z` = ?, `yaw` = ?, `pitch` = ?;")) {
            statement.setString(1, pName);
            statement.setString(2, pLocation.getServer());
            statement.setString(3, pLocation.getWorld());
            statement.setDouble(4, pLocation.getX());
            statement.setDouble(5, pLocation.getY());
            statement.setDouble(6, pLocation.getZ());
            statement.setFloat(7, pLocation.getYaw());
            statement.setFloat(8, pLocation.getPitch());
            statement.setBytes(9, toBytes(pOwner));
            statement.setString(10, pLocation.getServer());
            statement.setString(11, pLocation.getWorld());
            statement.setDouble(12, pLocation.getX());
            statement.setDouble(13, pLocation.getY());
            statement.setDouble(14, pLocation.getZ());
            statement.setFloat(15, pLocation.getYaw());
            statement.setFloat(16, pLocation.getPitch());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Error occurred.
    }

    @Override
    public boolean updateHome(int pHomeId, @NotNull HomeLocation pLocation) {
        try (final var connection = mSource.getConnection();
             final var statement = connection.prepareStatement(
                     "UPDATE `Homes` SET `server` = ?, `world` = ?, `x` = ?, `y` = ?, `z` = ?, `yaw` = ?, `pitch` = ? WHERE `homeId` = ?;")) {
            statement.setString(1, pLocation.getServer());
            statement.setString(2, pLocation.getWorld());
            statement.setDouble(3, pLocation.getX());
            statement.setDouble(4, pLocation.getY());
            statement.setDouble(5, pLocation.getZ());
            statement.setFloat(6, pLocation.getYaw());
            statement.setFloat(7, pLocation.getPitch());
            statement.setInt(8, pHomeId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Error occurred.
    }

    @Override
    public boolean deleteHome(@NotNull UUID pOwner, @NotNull String pName) {
        try (final var connection = mSource.getConnection();
             final var statement = connection.prepareStatement(
                     "DELETE FROM `Homes` WHERE `uniqueId` = ? AND `name` = ?;")) {
            statement.setBytes(1, toBytes(pOwner));
            statement.setString(2, pName);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Error occurred.
    }

    @Override
    public boolean deleteHome(int pHomeId) {
        try (final var connection = mSource.getConnection();
             final var statement = connection.prepareStatement(
                     "DELETE FROM `Homes` WHERE `homeId` = ?;")) {
            statement.setInt(1, pHomeId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Error occurred.
    }

    @Override
    public void registerPlayer(@NotNull UUID pOwner, @NotNull String pUsername) {
        try (final var connection = mSource.getConnection();
             final var statement = connection.prepareStatement(
                     "INSERT INTO `Players` VALUES (?, ?) ON DUPLICATE KEY UPDATE `username` = ?;")) {
            statement.setBytes(1, toBytes(pOwner));
            statement.setString(2, pUsername);
            statement.setString(3, pUsername);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @Nullable String getPlayerName(@NotNull final UUID pUniqueId) {
        try (final var connection = mSource.getConnection();
             final var statement = connection.prepareStatement(
                     "SELECT `username` FROM `Players` WHERE `uniqueId` = ?;")) {
            statement.setBytes(1, toBytes(pUniqueId));
            try (final var result = statement.executeQuery()) {
                if (result.next())
                    return result.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Some error occurred or that player is not registered.
    }

    @Override
    public @NotNull List<UUID> getPlayerUniqueId(@NotNull final String pUsername) {
        try (final var connection = mSource.getConnection();
             final var statement = connection.prepareStatement(
                     "SELECT `uniqueId` FROM `Players` WHERE `username` = ?;")) {
            statement.setString(1, pUsername);
            try (final var result = statement.executeQuery()) {
                final var uniqueIds = new ArrayList<UUID>();
                while (result.next())
                    uniqueIds.add(toUniqueId(result.getBytes(1)));
                return uniqueIds;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyList(); // Some error occurred or that player is not registered.
    }

    private static byte[] toBytes(@NotNull final UUID pUniqueId) {
        byte[] bytes = new byte[16];
        ByteBuffer.wrap(bytes)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(pUniqueId.getMostSignificantBits())
                .putLong(pUniqueId.getLeastSignificantBits());
        return bytes;
    }

    private static @NotNull UUID toUniqueId(final byte[] pBytes) {
        ByteBuffer buf = ByteBuffer.wrap(pBytes);
        return new UUID(buf.getLong(), buf.getLong());
    }

}
