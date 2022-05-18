package me.minecraft_server.homes.exceptions;

import me.minecraft_server.homes.util.HomeTarget;
import org.jetbrains.annotations.NotNull;

public class HomeNotFoundException extends RuntimeException {

    private static @NotNull String messageOf(@NotNull final String pHomeIdentifier) {
        return "The home " + pHomeIdentifier + " does not exist.";
    }

    public HomeNotFoundException(@NotNull final HomeTarget pHomeTarget) {
        super(messageOf(pHomeTarget.toHumanReadable()));
    }

    public HomeNotFoundException(@NotNull final HomeTarget pHomeTarget, @NotNull final Exception pCause) {
        super(messageOf(pHomeTarget.toHumanReadable()), pCause);
    }

}
