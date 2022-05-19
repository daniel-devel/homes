package me.minecraft_server.homes.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public final class CommandUtils {

    public static List<String> getPossibleCompletion(@NotNull String current, @NotNull Iterable<String> possibles) {
        final var list = new ArrayList<String>();
        for (final var possbile : possibles) {
            if (possbile.toLowerCase().startsWith(current.toLowerCase()))
                list.add(possbile);
        }
        return list;
    }

}
