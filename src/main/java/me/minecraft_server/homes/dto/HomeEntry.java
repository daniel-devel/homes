package me.minecraft_server.homes.dto;

import org.jetbrains.annotations.NotNull;

public record HomeEntry(int homeId, @NotNull String name, @NotNull HomeLocation location) {

    public @NotNull String toHumanReadable(final boolean pForeign) {
        return pForeign ? "@" + homeId : name;
    }

}
