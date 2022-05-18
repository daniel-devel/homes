package me.minecraft_server.homes.services;

import me.minecraft_server.homes.Homes;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class InvitationService implements Listener {

    @NotNull
    private final Homes mPlugin;

    public InvitationService(@NotNull final Homes pPlugin) {
        mPlugin = pPlugin;
    }

    @NotNull
    final Map<String, Map<Player, PendingInvitation>> sentInvitations = new HashMap<>();

    @NotNull
    final Map<Player, Map<String, PendingInvitation>> receivedInvitations = new HashMap<>();

    public @Nullable PendingInvitation getInvitation(@NotNull final String sender, @NotNull final Player player) {
        final var senderLC = sender.toLowerCase();
        final var senderMap = sentInvitations.get(senderLC);
        if (senderMap == null)
            return null;
        final var invitation = senderMap.get(player);
        if (invitation != null && invitation.expires < System.currentTimeMillis()) {
            senderMap.remove(player);
            if (senderMap.isEmpty())
                sentInvitations.remove(senderLC);
            removeReceivedInvitation(senderLC, player);
            return null;
        }
        return invitation;
    }

    public void createInvitation(@NotNull final CommandSender sender, @NotNull final Player target, @NotNull final Location bukkitLocation) {
        final var senderLC = sender.getName().toLowerCase();
        final var invitation = new PendingInvitation(sender, bukkitLocation, System.currentTimeMillis() + 120000L);
        sentInvitations.computeIfAbsent(senderLC, s -> new HashMap<>()).put(target, invitation);
        receivedInvitations.computeIfAbsent(target, t -> new HashMap<>()).put(senderLC, invitation);
    }

    public @Nullable PendingInvitation removeInvitation(String sender, Player target) {
        final var senderLC = sender.toLowerCase();
        removeSentInvitation(senderLC, target);
        return removeReceivedInvitation(senderLC, target);
    }

    private @Nullable PendingInvitation removeReceivedInvitation(@NotNull final String sender, @NotNull final Player target) {
        final var map = receivedInvitations.get(target);
        if (map == null)
            return null;
        final var inv = map.remove(sender);
        if (map.isEmpty())
            receivedInvitations.remove(target);
        return inv.expires > System.currentTimeMillis() ? inv : null;
    }

    private void removeSentInvitation(@NotNull final String sender, @NotNull final Player target) {
        final var map = sentInvitations.get(sender);
        if (map == null)
            return;
        map.remove(target);
        if (map.isEmpty())
            sentInvitations.remove(sender);
        // We don't return here, because we need just one helper method to return the invitation.
        // It doesn't matter which one.
    }

    @EventHandler
    private void OnPlayerQuit(PlayerQuitEvent e) {
        final var player = e.getPlayer();
        final var playerSender = player.getName().toLowerCase();
        sentInvitations.remove(playerSender).keySet().forEach(target -> removeReceivedInvitation(playerSender, target));
        receivedInvitations.remove(player).keySet().forEach(sender -> removeSentInvitation(sender, player));
    }

    public record PendingInvitation(@NotNull CommandSender sender, @NotNull Location location, long expires) { }

}
