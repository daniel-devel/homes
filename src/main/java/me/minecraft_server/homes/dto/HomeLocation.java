package me.minecraft_server.homes.dto;

import lombok.*;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Data @Accessors(prefix = "m")
public class HomeLocation {

    private double mX, mY, mZ;
    private float mYaw, mPitch;
    private @NotNull String mWorld;
    private @NotNull String mServer;


    /**
     * Constructs a home with the given parameters.
     * @param pX The x coordinate.
     * @param pY The y coordinate.
     * @param pZ The z coordinate.
     * @param pWorld The world coordinate.
     * @param pServer The server where the home is in.
     */
    public HomeLocation(final double pX, final double pY, final double pZ, final float pYaw, final float pPitch, @NotNull final String pWorld, @NotNull final String pServer) {
        mX = pX;
        mY = pY;
        mZ = pZ;
        mWorld = pWorld;
        mServer = pServer;
    }

    /**
     * Converts a bukkit location to a home location.
     * @param pBukkitLocation The bukkit location to convert.
     */
    public HomeLocation(@NotNull final Location pBukkitLocation, @NotNull final String pServer) {
        this(pBukkitLocation.getX(), pBukkitLocation.getY(), pBukkitLocation.getZ(),
                pBukkitLocation.getYaw(), pBukkitLocation.getPitch(),
                Objects.requireNonNull(pBukkitLocation.getWorld()).getName(), pServer);
    }

    public @Nullable Location toBukkitLocation() {
        final var world = Bukkit.getWorld(mWorld);
        if (world == null)
            return null;
        return new Location(world, mX, mY, mZ, mYaw, mPitch);
    }

}
