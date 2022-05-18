package me.minecraft_server.homes.commands;

import me.minecraft_server.homes.Homes;
import me.minecraft_server.homes.exceptions.NotUniquelyIdentifiableException;
import me.minecraft_server.homes.exceptions.RegisteredPlayerNotFoundException;
import me.minecraft_server.homes.util.HomeTarget;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DelHomeCommand implements CommandExecutor, TabCompleter {

    @NotNull
    private final Homes plugin;

    public DelHomeCommand(@NotNull final Homes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        final var homesService = plugin.getHomesService();
        final var syncExecutor = plugin.getSyncExecutor();
        final var asyncExecutor = plugin.getAsyncExecutor();

        // The home target to delete
        final var home = HomeTarget.parseString(args.length == 0 ? homesService.getDefaultHome() : args[0]);
        if (home == null) {
            sender.sendMessage(String.format("§5§lHomes §8| §cCould not parse home target '%s'", args[0]));
            return true;
        } else if (!(home instanceof HomeTarget.OwnHomeName || sender.hasPermission("homes.admin.edit"))) {
            sender.sendMessage("§5§lHomes §8| §cYou lack the permission to delete a foreign home target.");
            return true;
        }

        // Execute the actual location set
        CompletableFuture.supplyAsync(() -> homesService.deleteHomeLocation(home, (Player) sender), asyncExecutor).whenCompleteAsync((result, ex) -> {
            if (ex != null) {
                final var actual = ex.getCause();
                switch (actual) {
                    case NotUniquelyIdentifiableException ignored -> {
                        assert home instanceof HomeTarget.ForeignHomeName;
                        sender.sendMessage(String.format("§5§lHomes §8| §cThe player §d%s§c can't be uniquely identified.", ((HomeTarget.ForeignHomeName)home).owner()));
                    }
                    case RegisteredPlayerNotFoundException ignored -> {
                        assert home instanceof HomeTarget.ForeignHomeName;
                        sender.sendMessage(String.format("§5§lHomes §8| §cPlayer §d%s§c not found.", ((HomeTarget.ForeignHomeName)home).owner()));
                    }
                    case null, default ->
                            sender.sendMessage(String.format("§5§lHomes §8| §cCould not remove home §d%s§c, unknown exception.", home.toHumanReadable()));
                }
            } else if (result) {
                sender.sendMessage("§5§lHomes §8| §7The home §d" + home.toHumanReadable() + "§7 successfully removed!");
            } else {
                sender.sendMessage("§5§lHomes §8| §cThe home does not exist or an unknown error occurred.");
            }
        }, syncExecutor);

        // Exit
        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1)
            return plugin.getHomesService().getPossibleHomes(sender, args[0], "homes.admin.edit");
        return Collections.emptyList();
    }

}
