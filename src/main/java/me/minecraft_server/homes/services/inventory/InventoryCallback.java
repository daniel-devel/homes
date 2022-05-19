package me.minecraft_server.homes.services.inventory;

import me.minecraft_server.homes.services.InventoryService;
import org.bukkit.event.inventory.*;
import org.jetbrains.annotations.NotNull;

/**
 * This interface listens to events for specific inventories.
 * See {@link InventoryService} for further information.
 */
@SuppressWarnings("unused")
public interface InventoryCallback {

    /**
     * A callback for the click event.
     * @param pService An instance of the service calling the callback.
     * @param pEvent The click event forwarded to this callback.
     */
    default void onInventoryClick(@NotNull final InventoryService pService, @NotNull final InventoryClickEvent pEvent) { }

    /**
     * A callback for the drag event.
     * @param pService An instance of the service calling the callback.
     * @param pEvent The drag event forwarded to this callback.
     */
    default void onInventoryDrag(@NotNull final InventoryService pService, @NotNull final InventoryDragEvent pEvent) { }

    /**
     * A callback for the open event.
     * @param pService An instance of the service calling the callback.
     * @param pEvent The open event forwarded to this callback.
     */
    default void onInventoryOpen(@NotNull final InventoryService pService, @NotNull final InventoryOpenEvent pEvent) { }

    /**
     * A callback for the close event.
     * @param pService An instance of the service calling the callback.
     * @param pEvent The close event forwarded to this callback.
     */
    default void onInventoryClose(@NotNull final InventoryService pService, @NotNull final InventoryCloseEvent pEvent) { }

    /**
     * A callback for the creative event.
     * @param pService An instance of the service calling the callback.
     * @param pEvent The creative event forwarded to this callback.
     */
    default void onInventoryCreative(@NotNull final InventoryService pService, @NotNull final InventoryCreativeEvent pEvent) { }

}
