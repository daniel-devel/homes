package me.minecraft_server.homes.commands;

import lombok.RequiredArgsConstructor;
import me.minecraft_server.homes.Homes;
import me.minecraft_server.homes.exceptions.NotUniquelyIdentifiableException;
import me.minecraft_server.homes.exceptions.RegisteredPlayerNotFoundException;
import me.minecraft_server.homes.inventories.HomeListInventory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class HomesCommand implements CommandExecutor, TabCompleter {

    @NotNull
    private final Homes plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof final Player player)) {
            sender.sendMessage("§5§lHomes §8| §cOnly players are allowed to use this command!");
            return true;
        }

        final var homesService = plugin.getHomesService();
        final var syncExecutor = plugin.getSyncExecutor();
        final var asyncExecutor = plugin.getAsyncExecutor();

        if (args.length == 0 || !sender.hasPermission("homes.admin.info")) {
            CompletableFuture.supplyAsync(() -> homesService.getEntries(player.getUniqueId()), asyncExecutor)
                    .thenAcceptAsync(list -> HomeListInventory.openInventory(player, false, plugin, list), syncExecutor);
        } else if (args[0].length() > 16) try {
            final var uniqueId = UUID.fromString(args[0]);
            CompletableFuture.supplyAsync(() -> homesService.getEntries(uniqueId), asyncExecutor)
                    .thenAcceptAsync(list -> HomeListInventory.openInventory(player, false, plugin, list), syncExecutor);
        } catch (IllegalArgumentException ignored) {
            sender.sendMessage("§5§lHomes §8| §cCan't parse the unique id.");
        } else {
            CompletableFuture.supplyAsync(() -> homesService.getEntries(homesService.getPlayerName(args[0])), asyncExecutor)
                    .whenCompleteAsync((list, ex) -> {
                        if (ex != null) {
                            final var actual= ex.getCause();
                            switch (actual) {
                                case NotUniquelyIdentifiableException ignored ->
                                        sender.sendMessage(String.format("§5§lHomes §8| §cThe player §d%s§c can't be uniquely identified.", args[0]));
                                case RegisteredPlayerNotFoundException ignored ->
                                        sender.sendMessage(String.format("§5§lHomes §8| §cPlayer §d%s§c not found.", args[0]));
                                case null, default ->
                                        sender.sendMessage("§5§lHomes §8| §cUnknown error.");
                            }
                        } else {
                            HomeListInventory.openInventory(player, true, plugin, list);
                        }
                    }, syncExecutor);
        }

        // Exit.
        return true;

    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 && sender.hasPermission("homes.admin.info") ? null : Collections.emptyList();
    }

}
