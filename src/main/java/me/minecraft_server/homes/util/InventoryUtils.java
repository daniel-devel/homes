package me.minecraft_server.homes.util;

import lombok.experimental.UtilityClass;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@UtilityClass
public final class InventoryUtils {

    public static boolean isTopInventoryClicked(@NotNull final InventoryClickEvent pEvent) {
        return Objects.equals(pEvent.getWhoClicked().getOpenInventory().getTopInventory(), pEvent.getClickedInventory());
    }

}
