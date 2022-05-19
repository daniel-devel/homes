package me.minecraft_server.homes.services.inventory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.minecraft_server.homes.services.InventoryService;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

/**
 * This callback removes the inventory it listens to when it closes, if you don't intend to reuse it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CloseRemovalInventoryCallback implements InventoryCallback {

    // Singleton
    public static final CloseRemovalInventoryCallback INSTANCE = new CloseRemovalInventoryCallback();

    @Override
    public void onInventoryClose(@NotNull final InventoryService pService, @NotNull final InventoryCloseEvent pEvent) {
        pService.removeInventory(pEvent.getInventory());
    }
}
