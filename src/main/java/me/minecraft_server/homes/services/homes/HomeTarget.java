package me.minecraft_server.homes.services.homes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public sealed interface HomeTarget {

    record Identifier(int value) implements HomeTarget {

        @Override
        public @NotNull String toHumanReadable() {
            return "@" + value;
        }

        @Override
        public boolean isForeign() {
            return true;
        }

    }

    sealed interface HomeName extends HomeTarget {

        @NotNull String name();

    }

    record ForeignHomeName(@NotNull String name, @NotNull String owner) implements HomeName {

        @Override
        public @NotNull String toHumanReadable() {
            return owner + ":" + name;
        }

        @Override
        public boolean isForeign() {
            return true;
        }

    }

    record ForeignHomeNameUnique(@NotNull String name, @NotNull UUID owner) implements HomeName {

        @Override
        public @NotNull String toHumanReadable() {
            return owner + ":" + name;
        }

        @Override
        public boolean isForeign() {
            return true;
        }

    }

    record OwnHomeName(@NotNull String name) implements HomeName {

        @Override
        public @NotNull String toHumanReadable() {
            return name;
        }

        @Override
        public boolean isForeign() {
            return false;
        }

    }

    /**
     * Parses the string to a home target.
     * @param pStringTarget The string to parse.
     * @return A home target to that string. Can be null, which means the string could not be parsed successfully.
     */
    static @Nullable HomeTarget parseString(@NotNull final String pStringTarget) {
        if (pStringTarget.startsWith("@"))
            try {
                return new Identifier(Integer.parseInt(pStringTarget.substring(1)));
            } catch (NumberFormatException e) {
                return null;
            }
        final var split = pStringTarget.split(":", 2);
        if (split.length == 1)
            return new OwnHomeName(split[0]);
        else if (split[0].length() <= 16)
            return new ForeignHomeName(split[1], split[0]);
        else try {
            return new ForeignHomeNameUnique(split[1], UUID.fromString(split[0]));
        } catch (IllegalArgumentException ignored) {
            return null;
        }

    }

    /**
     * Converts the target to a human-readable from.
     * @return The converted string.
     */
    @NotNull String toHumanReadable();

    /**
     * @return Whether this target can be parsed without a player.
     *         Used to determine if console can use this or not.
     */
    boolean isForeign();

}
