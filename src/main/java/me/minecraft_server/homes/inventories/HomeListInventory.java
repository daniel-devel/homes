package me.minecraft_server.homes.inventories;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.minecraft_server.homes.Homes;
import me.minecraft_server.homes.dto.HomeEntry;
import me.minecraft_server.homes.services.InventoryService;
import me.minecraft_server.homes.services.inventory.CloseRemovalInventoryCallback;
import me.minecraft_server.homes.services.inventory.InventoryCallback;
import me.minecraft_server.homes.services.inventory.StaticInventoryCallback;
import me.minecraft_server.homes.util.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Accessors(prefix = "m")
public class HomeListInventory implements InventoryCallback {

    private int mPage = 0;
    private final int mLastPage;
    private static final int PAGE_SIZE = 27;

    private final @NotNull Inventory mInventory = Bukkit.createInventory(null, PAGE_SIZE + 9, "§5§lHomes");

    @Getter(AccessLevel.PROTECTED)
    private final boolean mForeign;

    @Getter(AccessLevel.PROTECTED)
    private final Homes mPlugin;

    private HomeListInventory(final boolean pForeign, @NotNull final Homes pPlugin, @NotNull final List<HomeEntry> pEntries) {

        mPlugin = pPlugin;
        mForeign = pForeign;
        mEntries = pEntries;
        mEntryItems = new ArrayList<>(mEntries.size());
        mLastPage = (mEntries.size() - 1) / PAGE_SIZE ;

        // Fill second page with null items, they will be created later, if needed.
        for (int i = 0; i < mEntries.size(); i++)
            mEntryItems.add(null);

        // Fill in first page.
        fillInventory();

        // Fill in placeholder items that never change.
        for (int slot = 28; slot < 35; slot++)
            mInventory.setItem(slot, PLACEHOLDER_ITEM);

    }

    private final @NotNull List<HomeEntry> mEntries;
    private final @NotNull List<@Nullable ItemStack> mEntryItems;

    private static final ItemStack PLACEHOLDER_ITEM = new ItemStack(Material.BLACK_STAINED_GLASS_PANE) {{
        final var meta = getItemMeta();
        assert meta != null;
        meta.setDisplayName("§0");
        setItemMeta(meta);
    }};

    private static final ItemStack BACK_ITEM = new ItemStack(Material.ARROW) {{
        final var meta = getItemMeta();
        assert meta != null;
        meta.setDisplayName("§aLast Page");
        setItemMeta(meta);
    }};

    private static final ItemStack FORWARD_ITEM = new ItemStack(Material.ARROW) {{
        final var meta = getItemMeta();
        assert meta != null;
        meta.setDisplayName("§aNext Page");
        setItemMeta(meta);
    }};

    private @NotNull ItemStack createItem(@NotNull final HomeEntry entry) {
        final var item = new ItemStack(Material.PAPER);
        final var meta = item.getItemMeta();
        assert meta != null; // I don't know how this can be ever null.
        meta.setDisplayName(String.format("§5%s §7(§d%d§7)", entry.name(), entry.homeId()));
        final var lore = new ArrayList<String>();
        if (isOnSameServer(entry))
            lore.add("§7Left-click to teleport");
        lore.add("§7Right-click to modify");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private @NotNull ItemStack getItem(final int index) {
        var item = mEntryItems.get(index);
        if (item == null) {
            item = createItem(mEntries.get(index));
            mEntryItems.set(index, item);
        }
        return item;
    }

    private void fillInventory() {
        final int firstIndex = mPage * PAGE_SIZE;
        final int lastIndex = Math.min((mPage + 1) * PAGE_SIZE, mEntries.size()) - 1;
        final int lastSlot = lastIndex - firstIndex;
        for (int slot = 0; slot <= lastSlot; slot++)
            mInventory.setItem(slot, getItem(slot + firstIndex));
        for (int slot = lastSlot + 1; slot < PAGE_SIZE; slot++)
            mInventory.setItem(slot, null);
        mInventory.setItem(27, mPage > 0 ? BACK_ITEM : PLACEHOLDER_ITEM);
        mInventory.setItem(35, mLastPage > mPage ? FORWARD_ITEM : PLACEHOLDER_ITEM);
    }

    @Override
    public void onInventoryClick(@NotNull InventoryService pService, @NotNull InventoryClickEvent pEvent) {

        // Ensure the top inventory is clicked. We don't care about the users inventory.
        if (!InventoryUtils.isTopInventoryClicked(pEvent))
            return;

        // Ensure the slot clicked has an entry.
        final var slot = pEvent.getSlot();

        if (slot == 27 && mPage > 0) {
            mPage -= 1;
            fillInventory();
            return;
        }

        if (slot == 35 && mLastPage > mPage) {
            mPage += 1;
            fillInventory();
            return;
        }

        final int firstIndex = mPage * PAGE_SIZE;
        final int indexLength = mEntries.size() - firstIndex;
        if (slot < 0 || slot >= indexLength)
            return;

        // Get the target entry.
        final var player = (Player) pEvent.getWhoClicked();
        final var entry = mEntries.get(firstIndex + slot);
        if (pEvent.isLeftClick() && isOnSameServer(entry)) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "home " + entry.toHumanReadable(mForeign));
        } else if (pEvent.isRightClick()) {
            player.closeInventory();
            HomeModifyInventory.openInventory(player, entry, this);
        }

    }

    protected boolean isOnSameServer(@NotNull final HomeEntry pEntry) {
        return pEntry.location().getServer().equalsIgnoreCase(mPlugin.getHomesService().getServer());
    }

    public static void openInventory(@NotNull final Player pPlayer, @NotNull final HomeListInventory pInstance) {
        pInstance.mPlugin.getInventoryService().addInventoryCallback(pInstance.mInventory, pInstance, CloseRemovalInventoryCallback.INSTANCE, StaticInventoryCallback.INSTANCE);
        pPlayer.openInventory(pInstance.mInventory);
    }

    public static void openInventory(@NotNull final Player pPlayer, final boolean pForeign, @NotNull final Homes pPlugin, @NotNull final List<HomeEntry> pEntries) {
        openInventory(pPlayer, new HomeListInventory(pForeign, pPlugin, pEntries));
    }

}
