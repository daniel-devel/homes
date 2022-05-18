package me.minecraft_server.homes.dto;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class HomeLocation {

    private double x, y, z;
    private float yaw, pitch;
    private @NotNull String world;
    private @NotNull String server;


    /**
     * Constructs a home with the given parameters.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @param world The world coordinate.
     * @param server The server where the home is in.
     */
    public HomeLocation(final double x, final double y, final double z, final float yaw, final float pitch, @NotNull final String world, @NotNull final String server) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
        this.server = server;
    }

    /**
     * Converts a bukkit location to a home location.
     * @param bukkitLocation The bukkit location to convert.
     */
    public HomeLocation(@NotNull final Location bukkitLocation, @NotNull final String server) {
        this(bukkitLocation.getX(), bukkitLocation.getY(), bukkitLocation.getZ(),
                bukkitLocation.getYaw(), bukkitLocation.getPitch(),
                Objects.requireNonNull(bukkitLocation.getWorld()).getName(), server);
    }

    public @Nullable Location toBukkitLocation() {
        final var world = Bukkit.getWorld(this.world);
        if (world == null)
            return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    public double getX() {
        return x;
    }

    public void setX(final double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(final double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(final double z) {
        this.z = z;
    }

    public @NotNull String getServer() {
        return server;
    }

    public void setServer(@NotNull final String server) {
        this.server = server;
    }

    public @NotNull String getWorld() {
        return world;
    }

    public void setWorld(@NotNull final String world) {
        this.world = world;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HomeLocation that = (HomeLocation) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0 && Double.compare(that.z, z) == 0 && Float.compare(that.yaw, yaw) == 0 && Float.compare(that.pitch, pitch) == 0 && world.equals(that.world) && server.equals(that.server);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, yaw, pitch, world, server);
    }

    @Override
    public String toString() {
        return "HomeLocation{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                ", world='" + world + '\'' +
                ", server='" + server + '\'' +
                '}';
    }

}
