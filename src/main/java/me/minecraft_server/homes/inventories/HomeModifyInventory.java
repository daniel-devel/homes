package me.minecraft_server.homes.inventories;

import me.minecraft_server.homes.dto.HomeEntry;
import me.minecraft_server.homes.services.InventoryService;
import me.minecraft_server.homes.services.inventory.CloseRemovalInventoryCallback;
import me.minecraft_server.homes.services.inventory.InventoryCallback;
import me.minecraft_server.homes.services.inventory.StaticInventoryCallback;
import me.minecraft_server.homes.util.InventoryUtils;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;

public class HomeModifyInventory implements InventoryCallback {

    private final @NotNull HomeEntry mEntry;
    private final @NotNull HomeListInventory mParent;

    public HomeModifyInventory(@NotNull final HomeEntry pEntry, @NotNull final HomeListInventory pParent) {
        mEntry = pEntry;
        mParent = pParent;
    }

    @Override
    public void onInventoryClick(@NotNull InventoryService pService, @NotNull InventoryClickEvent pEvent) {

        // Ensure the top inventory is clicked. We don't care about the users inventory.
        if (!InventoryUtils.isTopInventoryClicked(pEvent))
            return;

        // Ensure the item was left-clicked.
        if (!pEvent.isLeftClick())
            return;

        final var player = (Player) pEvent.getWhoClicked();
        switch (pEvent.getSlot()) {
            case 2 -> {
                player.closeInventory();
                Bukkit.dispatchCommand(player, "home " + mEntry.toHumanReadable(mParent.isForeign()));
            }

            case 3 -> {
                player.closeInventory();
                Bukkit.dispatchCommand(player, "sethome " + mEntry.toHumanReadable(mParent.isForeign()) + " override");
            }

            case 4 -> {
                player.closeInventory();
                new AnvilGUI.Builder()
                        .plugin(mParent.getPlugin())
                        .itemLeft(INVITE_ANVIL_ITEM)
                        .onComplete((ignored, input) -> {
                            if (!input.isBlank())
                                Bukkit.dispatchCommand(player, "invhome " + mEntry.toHumanReadable(mParent.isForeign()) + " " + input);
                            return AnvilGUI.Response.close();
                        }).open(player);
            }

            case 6 -> {
                player.closeInventory();
                Bukkit.dispatchCommand(player, "delhome " + mEntry.toHumanReadable(mParent.isForeign()));
            }

            case 8 -> {
                player.closeInventory();
                HomeListInventory.openInventory(player, mParent);
            }

        }

    }

    private static final ItemStack TELEPORT_ITEM = new ItemStack(Material.ENDER_PEARL) {{
        final var meta = getItemMeta();
        assert meta != null;
        meta.setDisplayName("§dTeleport");
        meta.setLore(Collections.singletonList("§7Left-click to teleport."));
        setItemMeta(meta);
    }};

    private static final ItemStack TELEPORT_LOCKED_ITEM = new ItemStack(Material.ENDER_PEARL) {{
        final var meta = getItemMeta();
        assert meta != null;
        meta.setDisplayName("§dTeleport §8(§4LOCKED§8)");
        meta.setLore(Collections.singletonList("§cThe home points to a different server."));
        setItemMeta(meta);
    }};

    private static final ItemStack SET_ITEM = new ItemStack(Material.EMERALD) {{
        final var meta = getItemMeta();
        assert meta != null;
        meta.setDisplayName("§eSet Location");
        meta.setLore(Collections.singletonList("§7Left-click to set location."));
        setItemMeta(meta);
    }};

    private static final ItemStack INVITE_ITEM = new ItemStack(Material.WRITABLE_BOOK) {{
        final var meta = getItemMeta();
        assert meta != null;
        meta.setDisplayName("§bInvite");
        meta.setLore(Collections.singletonList("§7Left-click to invite someone."));
        setItemMeta(meta);
    }};

    private static final ItemStack DELETE_ITEM = new ItemStack(Material.CAMPFIRE) {{
        final var meta = getItemMeta();
        assert meta != null;
        meta.setDisplayName("§cDelete");
        meta.setLore(Collections.singletonList("§7Left-click to delete."));
        setItemMeta(meta);
    }};

    private static final ItemStack BACK_ITEM = new ItemStack(Material.BARRIER) {{
        final var meta = getItemMeta();
        assert meta != null;
        meta.setDisplayName("§8Back");
        meta.setLore(Collections.singletonList("§7Left-click to go back."));
        setItemMeta(meta);
    }};

    private static final ItemStack PLACEHOLDER_ITEM = new ItemStack(Material.BLACK_STAINED_GLASS_PANE) {{
        final var meta = getItemMeta();
        assert meta != null;
        meta.setDisplayName("§0");
        setItemMeta(meta);
    }};

    private static final ItemStack INVITE_ANVIL_ITEM = new ItemStack(Material.PAPER) {{
        final var meta = getItemMeta();
        assert meta != null;
        meta.setDisplayName("Username");
        setItemMeta(meta);
    }};

    public static void openInventory(@NotNull final Player pPlayer, @NotNull final HomeEntry pEntry, @NotNull final HomeListInventory pParent) {
        final var callback = new HomeModifyInventory(pEntry, pParent);
        final var inventory = Bukkit.createInventory(null, 9, "§5§lModify §d" + pEntry.name());
        inventory.setItem(0, new ItemStack(Material.PAPER) {{
            final var meta = getItemMeta();
            assert meta != null;
            meta.setDisplayName("§5Info");
            final var location = pEntry.location();
            meta.setLore(Arrays.asList(
                "§7Name: §d" + pEntry.name(),
                "§7ID: §d" + pEntry.homeId(),
                "§7Server: §d" + location.getServer(),
                "§7World: §d" + location.getWorld(),
                String.format("§7Coordinates: §d%f§7, §d%f§7, §d%f§7",
                        location.getX(), location.getY(), location.getZ()),
                String.format("§7Orientation: §d%f§7, §d%f§7",
                        location.getYaw(), location.getPitch())
            ));
            setItemMeta(meta);
        }});
        inventory.setItem(1, PLACEHOLDER_ITEM);
        inventory.setItem(2, pParent.isOnSameServer(pEntry) ? TELEPORT_ITEM : TELEPORT_LOCKED_ITEM);
        inventory.setItem(3, SET_ITEM);
        inventory.setItem(4, INVITE_ITEM);
        inventory.setItem(5, PLACEHOLDER_ITEM);
        inventory.setItem(6, DELETE_ITEM);
        inventory.setItem(7, PLACEHOLDER_ITEM);
        inventory.setItem(8, BACK_ITEM);
        pParent.getPlugin().getInventoryService().addInventoryCallback(inventory, callback, CloseRemovalInventoryCallback.INSTANCE, StaticInventoryCallback.INSTANCE);
        pPlayer.openInventory(inventory);
    }

}
