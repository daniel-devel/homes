package me.minecraft_server.homes.services;

import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
public final class InvitationService implements Listener {

    @NotNull
    private final Homes mPlugin;

    @NotNull
    final Map<String, Map<Player, PendingInvitation>> mSentInvitations = new HashMap<>();

    @NotNull
    final Map<Player, Map<String, PendingInvitation>> mReceivedInvitations = new HashMap<>();

    public @Nullable PendingInvitation getInvitation(@NotNull final String pSender, @NotNull final Player pTarget) {
        final var senderLC = pSender.toLowerCase();
        final var senderMap = mSentInvitations.get(senderLC);
        if (senderMap == null)
            return null;
        final var invitation = senderMap.get(pTarget);
        if (invitation != null && invitation.expires < System.currentTimeMillis()) {
            senderMap.remove(pTarget);
            if (senderMap.isEmpty())
                mSentInvitations.remove(senderLC);
            removeReceivedInvitation(senderLC, pTarget);
            return null;
        }
        return invitation;
    }

    public void createInvitation(@NotNull final CommandSender pSender, @NotNull final Player pTarget, @NotNull final Location pLocation) {
        final var senderLC = pSender.getName().toLowerCase();
        final var invitation = new PendingInvitation(pSender, pLocation, System.currentTimeMillis() + 120000L);
        mSentInvitations.computeIfAbsent(senderLC, s -> new HashMap<>()).put(pTarget, invitation);
        mReceivedInvitations.computeIfAbsent(pTarget, t -> new HashMap<>()).put(senderLC, invitation);
    }

    public @Nullable PendingInvitation removeInvitation(String pSender, Player pTarget) {
        final var senderLC = pSender.toLowerCase();
        removeSentInvitation(senderLC, pTarget);
        return removeReceivedInvitation(senderLC, pTarget);
    }

    private @Nullable PendingInvitation removeReceivedInvitation(@NotNull final String pSender, @NotNull final Player pTarget) {
        final var map = mReceivedInvitations.get(pTarget);
        if (map == null)
            return null;
        final var inv = map.remove(pSender);
        if (map.isEmpty())
            mReceivedInvitations.remove(pTarget);
        return inv.expires > System.currentTimeMillis() ? inv : null;
    }

    private void removeSentInvitation(@NotNull final String sender, @NotNull final Player target) {
        final var map = mSentInvitations.get(sender);
        if (map == null)
            return;
        map.remove(target);
        if (map.isEmpty())
            mSentInvitations.remove(sender);
        // We don't return here, because we need just one helper method to return the invitation.
        // It doesn't matter which one.
    }

    @EventHandler
    private void OnPlayerQuit(PlayerQuitEvent e) {
        final var player = e.getPlayer();
        final var playerSender = player.getName().toLowerCase();
        mSentInvitations.remove(playerSender).keySet().forEach(target -> removeReceivedInvitation(playerSender, target));
        mReceivedInvitations.remove(player).keySet().forEach(sender -> removeSentInvitation(sender, player));
    }

    public record PendingInvitation(@NotNull CommandSender sender, @NotNull Location location, long expires) { }

}
