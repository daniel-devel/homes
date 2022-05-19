package me.minecraft_server.homes.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import me.minecraft_server.homes.services.inventory.InventoryCallback;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * This manager distributes inventory events to inventory callbacks.
 */
public final class InventoryService implements Listener {

    /**
     * A list of currently listening callbacks to an inventory.
     */
    private final HashMultimap<Inventory, InventoryCallback> mInventoryCallbacks = HashMultimap.create();

    /**
     * Adds {@code callback} to {@code inventory}.
     * @param pInventory The target inventory that the callback(s) should listen to.
     * @param pCallbacks The inventory callback(s) that should be executed when the inventory issues an event.
     */
    public synchronized void addInventoryCallback(@NotNull final Inventory pInventory, @NotNull final InventoryCallback... pCallbacks) {
        mInventoryCallbacks.putAll(pInventory, Arrays.asList(pCallbacks));
    }

    /**
     * Removes {@code callback} from {@code inventory}.
     * @param pInventory The target inventory.
     * @param pCallback The target callback.
     */
    public synchronized void removeInventoryCallback(@NotNull final Inventory pInventory, @NotNull final InventoryCallback pCallback) {
        mInventoryCallbacks.remove(pInventory, pCallback);
    }

    /**
     * Removes all callbacks from {@code inventory}.
     * @param pInventory The target inventory.
     */
    public synchronized void removeInventory(@NotNull final Inventory pInventory) {
        mInventoryCallbacks.removeAll(pInventory);
    }

    // IMPLEMENTATION: Listener

    /**
     * Retrieves the callbacks of the current inventory and executes them.
     * It's ensured that modifying the callbacks during the execution is possible, creating a copy of that list.
     * @param pClicked The clicked inventory whose callbacks to execute.
     * @param pTask The task to execute on the callbacks.
     */
    private synchronized void execute(@NotNull final Inventory pClicked, @NotNull final Consumer<@NotNull InventoryCallback> pTask) {
        ImmutableList.copyOf(mInventoryCallbacks.get(pClicked)).forEach(pTask);
    }

    @EventHandler
    private void onInventoryDrag(@NotNull final InventoryDragEvent pEvent) {
        execute(pEvent.getWhoClicked().getOpenInventory().getTopInventory(), callback -> callback.onInventoryDrag(this, pEvent));
    }

    @EventHandler
    private void onInventoryClick(@NotNull final InventoryClickEvent pEvent) {
        execute(pEvent.getWhoClicked().getOpenInventory().getTopInventory(), callback -> callback.onInventoryClick(this, pEvent));
    }

    @EventHandler
    private void onInventoryCreative(@NotNull final InventoryCreativeEvent pEvent) {
        execute(pEvent.getWhoClicked().getOpenInventory().getTopInventory(), callback -> callback.onInventoryCreative(this, pEvent));
    }

    @EventHandler
    private void onInventoryOpen(@NotNull final InventoryOpenEvent pEvent) {
        execute(pEvent.getInventory(), callback -> callback.onInventoryOpen(this, pEvent));
    }

    @EventHandler
    private void onInventoryClose(@NotNull final InventoryCloseEvent pEvent) {
        execute(pEvent.getInventory(), callback -> callback.onInventoryClose(this, pEvent));
    }

}
