package me.minecraft_server.homes.services.inventory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.minecraft_server.homes.services.InventoryService;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.jetbrains.annotations.NotNull;

/**
 * This callback deactivates moving items around in the inventory it listens to.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StaticInventoryCallback implements InventoryCallback {

    // Singleton
    public static final StaticInventoryCallback INSTANCE = new StaticInventoryCallback();

    @Override
    public void onInventoryClick(final @NotNull InventoryService pService, final @NotNull InventoryClickEvent pEvent) {
        pEvent.setResult(Event.Result.DENY);
        pEvent.setCancelled(true);
    }

    @Override
    public void onInventoryDrag(final @NotNull InventoryService pService, final @NotNull InventoryDragEvent pEvent) {
        pEvent.setResult(Event.Result.DENY);
        pEvent.setCancelled(true);
    }

    @Override
    public void onInventoryCreative(final @NotNull InventoryService pService, final @NotNull InventoryCreativeEvent pEvent) {
        pEvent.setResult(Event.Result.DENY);
        pEvent.setCancelled(true);
    }

}