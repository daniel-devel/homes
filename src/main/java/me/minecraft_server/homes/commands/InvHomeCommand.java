package me.minecraft_server.homes.commands;

import lombok.RequiredArgsConstructor;
import me.minecraft_server.homes.Homes;
import me.minecraft_server.homes.exceptions.HomeNotFoundException;
import me.minecraft_server.homes.exceptions.NotUniquelyIdentifiableException;
import me.minecraft_server.homes.exceptions.RegisteredPlayerNotFoundException;
import me.minecraft_server.homes.services.homes.HomeTarget;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
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
public class InvHomeCommand implements CommandExecutor, TabCompleter {

    @NotNull
    private final Homes plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        final var homesService = plugin.getHomesService();
        final var syncExecutor = plugin.getSyncExecutor();
        final var asyncExecutor = plugin.getAsyncExecutor();
        final var invitationService = plugin.getInvitationService();

        // Who is the initiator?
        final var initiator = sender instanceof Player ? (Player) sender : null;

        if (args.length <= 1) {
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

        for (int i = 1; i < args.length; i++) {

            // Who are we inviting?
            final Player target = Bukkit.getPlayer(args[i]);
            if (target == null) {
                sender.sendMessage(String.format("§5§lHomes §8| §cThe player %s is not online.", args[i]));
                continue;
            }

            if (target == sender) {
                sender.sendMessage("§5§lHomes §8| §cYou can't invite yourself.");
                continue;
            }

            if (invitationService.getInvitation(sender.getName(), target) != null) {
                sender.sendMessage(String.format("§5§lHomes §8| §cYou already invited §d%s§c, please wait two minutes until the invitation expires!", target.getDisplayName()));
                continue;
            }

            // Get the location
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
                    } else {
                        invitationService.createInvitation(sender, target, bukkitLocation);
                        sender.sendMessage(String.format("§5§lHomes §8| §7You sent an invitation to §d%s§7! The invitation expires in two minutes.", target.getDisplayName()));
                        target.spigot().sendMessage(
                                new TextComponent(new ComponentBuilder("Homes").color(ChatColor.DARK_PURPLE).bold(true)
                                        .append(" | ").color(ChatColor.DARK_GRAY).bold(false)
                                        .append("You got an invite to a home from ").color(ChatColor.GRAY)
                                        .append(sender.getName()).color(ChatColor.LIGHT_PURPLE)
                                        .append(", you can either ").color(ChatColor.GRAY).create()),
                                new TextComponent(new ComponentBuilder("ACCEPT").color(ChatColor.GREEN).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/acpthome " + sender.getName()))
                                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click here to accept the request."))).create()),
                                new TextComponent(new ComponentBuilder(" or ").color(ChatColor.GRAY)
                                        .append("DECLINE").color(ChatColor.RED).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/declhome " + sender.getName()))
                                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click here to decline the request."))).create()),
                                new TextComponent(new ComponentBuilder(" this request. The request will expire in two minutes.").color(ChatColor.GRAY).create()));
                    }
                }
            }, syncExecutor);

        }

        // Exit
        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return switch (args.length) {
            case 1 -> plugin.getHomesService().getPossibleHomes(sender, args[0], "homes.admin.use");
            case 2 -> null;
            default -> Collections.emptyList();
        };
    }

}
