package me.minecraft_server.homes.exceptions;

import org.jetbrains.annotations.NotNull;

public class NotUniquelyIdentifiableException extends RuntimeException {

    public NotUniquelyIdentifiableException(@NotNull final String pPlayerName) {
        super("Multiple players with the name '" + pPlayerName + "' are registered. Please use unique ids instead.");
    }

}
