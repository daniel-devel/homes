package me.minecraft_server.homes.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Identifier that = (Identifier) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ForeignHomeName that = (ForeignHomeName) o;
            return name.equalsIgnoreCase(that.name) && owner.equalsIgnoreCase(that.owner);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name.toLowerCase(), owner.toLowerCase());
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ForeignHomeNameUnique that = (ForeignHomeNameUnique) o;
            return name.equalsIgnoreCase(that.name) && owner.equals(that.owner);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name.toLowerCase(), owner);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OwnHomeName that = (OwnHomeName) o;
            return name.equalsIgnoreCase(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name.toLowerCase());
        }

    }

    static @Nullable HomeTarget parseString(@NotNull final String unknownId) {
        if (unknownId.startsWith("@"))
            try {
                return new Identifier(Integer.parseInt(unknownId.substring(1)));
            } catch (NumberFormatException e) {
                return null;
            }
        final var split = unknownId.split(":", 2);
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

    @NotNull String toHumanReadable();

    boolean isForeign();

}
