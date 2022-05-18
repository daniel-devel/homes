package me.minecraft_server.homes.commands;

import me.minecraft_server.homes.Homes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class AcptHomeCommand implements CommandExecutor, TabCompleter {

    @NotNull
    private final Homes plugin;

    public AcptHomeCommand(@NotNull final Homes pPlugin) {
        plugin = pPlugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§5§lHomes §8| §cOnly players can get home invites!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§5§lHomes §8| §cYou did not specify what invite you are accepting, usage: §d" + command.getUsage().replaceAll("<command>", label));
            return true;
        }

        final var invitationService = plugin.getInvitationService();
        final var teleportService = plugin.getTeleportService();

        final var invite = invitationService.removeInvitation(args[0], (Player) sender);

        if (invite == null) {
            sender.sendMessage("§5§lHomes §8| §cThis invite does not exist or is expired.");
            return true;
        }

        invite.sender().sendMessage(String.format("§5§lHomes §8| §7The player §d%s§7 accepted your invite!", ((Player) sender).getDisplayName()));
        sender.sendMessage("§5§lHomes §8| §7You successfully accepted the invite. You will be teleported in five seconds, don't move!");
        teleportService.teleport((Player) sender, invite.location());

        // Exit
        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? null : Collections.emptyList();
    }

}
