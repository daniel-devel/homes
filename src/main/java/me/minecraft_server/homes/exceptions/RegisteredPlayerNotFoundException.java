package me.minecraft_server.homes.exceptions;

import org.jetbrains.annotations.NotNull;

public class RegisteredPlayerNotFoundException extends RuntimeException {

    private static @NotNull String messageOf(@NotNull final String pPlayerName) {
        return "No player found who is registered with the name '" + pPlayerName + "'. Try to use unique ids instead.";
    }

    public RegisteredPlayerNotFoundException(@NotNull final String pPlayerName) {
        super(messageOf(pPlayerName));
    }

    public RegisteredPlayerNotFoundException(@NotNull final String pPlayerName, @NotNull final Exception pCause) {
        super(messageOf(pPlayerName), pCause);
    }

}
