package me.minecraft_server.homes.commands;

import lombok.RequiredArgsConstructor;
import me.minecraft_server.homes.Homes;
import me.minecraft_server.homes.exceptions.HomeNotFoundException;
import me.minecraft_server.homes.exceptions.NotUniquelyIdentifiableException;
import me.minecraft_server.homes.exceptions.RegisteredPlayerNotFoundException;
import me.minecraft_server.homes.services.homes.HomeTarget;
import org.bukkit.Bukkit;
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

@RequiredArgsConstructor
public class HomeCommand implements CommandExecutor, TabCompleter {

    @NotNull
    private final Homes plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        final var homesService = plugin.getHomesService();
        final var syncExecutor = plugin.getSyncExecutor();
        final var asyncExecutor = plugin.getAsyncExecutor();
        final var teleportService = plugin.getTeleportService();

        // Who is the initiator?
        final var initiator = sender instanceof Player ? (Player) sender : null;

        if (args.length <= 1 && !(sender instanceof Player)) {
            sender.sendMessage(String.format("§5§lHomes §8| §cNot enough arguments, usage: %s", command.getUsage().replaceAll("<command>", label)));
            return true;
        }

        // The home target to teleport to
        final var home = HomeTarget.parseString(args.length == 0 ? homesService.getDefaultHome() : args[0]);

        if (home == null) {
            sender.sendMessage(String.format("§5§lHomes §8| §cCould not parse home target '%s'", args[0]));
            return true;
        }

        if (home.isForeign() && !sender.hasPermission("homes.admin.use")) {
            sender.sendMessage("§5§lHomes §8| §cYou lack the permission to use a foreign home target.");
            return true;
        }

        if (!(home.isForeign() || sender instanceof Player)) {
            sender.sendMessage("§5§lHomes §8| §cThis home target can only be used by players, please use a foreign home target.");
            return true;
        }

        // Who are we teleporting?
        final Player teleportTarget;
        if (args.length <= 1 || !sender.hasPermission("homes.admin.teleport")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§5§lHomes §8| §cYou are not a player and lacking permission to use foreign home targets. You are unable to use this command at all.");
                return true;
            }
            teleportTarget = (Player) sender;
        } else {
            teleportTarget = Bukkit.getPlayer(args[1]);
            if (teleportTarget == null) {
                sender.sendMessage(String.format("§5§lHomes §8| §cThe player %s is not online.", args[1]));
                return true;
            }
        }

        // If the player can teleport instantly or has to wait five seconds.
        final var instant = teleportTarget != sender || sender.hasPermission("homes.instant");

        // Execute the actual teleport
        CompletableFuture.supplyAsync(() -> homesService.getHomeLocation(home, initiator), asyncExecutor).whenCompleteAsync((location, ex) -> {
            if (ex != null) {
                final var actual= ex.getCause();
                switch (actual) {
                    case HomeNotFoundException ignored ->
                            sender.sendMessage(String.format("§5§lHomes §8| §cCan't find home §d%s§c.", home.toHumanReadable()));
                    case NotUniquelyIdentifiableException ignored ->
                            sender.sendMessage(String.format("§5§lHomes §8| §cThe player §d%s§c can't be uniquely identified.", ((HomeTarget.ForeignHomeName)home).owner()));
                    case RegisteredPlayerNotFoundException ignored ->
                            sender.sendMessage(String.format("§5§lHomes §8| §cPlayer §d%s§c not found.", ((HomeTarget.ForeignHomeName)home).owner()));
                    case null, default ->
                            sender.sendMessage(String.format("§5§lHomes §8| §cCan't retrieve home §d%s§c, unknown exception.", home.toHumanReadable()));
                }
            } else if (!location.getServer().equals(homesService.getServer())) {
                sender.sendMessage("§5§lHomes §8| §cThe home " + home.toHumanReadable() + " exists, but it's located on different server: " + location.getServer());
            } else {
                final var bukkitLocation = location.toBukkitLocation();
                if (bukkitLocation == null) {
                    sender.sendMessage("§5§lHomes §8| §cThe target home is invalid, the world it points to does not exist anymore.");
                } else if (instant) {
                    teleportTarget.teleport(bukkitLocation);
                } else {
                    teleportService.teleport(teleportTarget, bukkitLocation);
                    sender.sendMessage("§5§lHomes §8| §7You will be teleported in 5 seconds, don't move.");
                }
            }
        }, syncExecutor);

        // Exit
        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return switch (args.length) {
            case 1 -> plugin.getHomesService().getPossibleHomes(sender, args[0], "homes.admin.use");
            case 2 -> sender.hasPermission("homes.admin.teleport") ? null : Collections.emptyList();
            default -> Collections.emptyList();
        };
    }

}
