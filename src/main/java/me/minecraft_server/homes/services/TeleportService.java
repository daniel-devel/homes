package me.minecraft_server.homes.services;

import lombok.RequiredArgsConstructor;
import me.minecraft_server.homes.Homes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public final class TeleportService implements Listener {

    @NotNull
    private final Homes plugin;

    @NotNull
    private final Map<Player, PendingTeleport> pendingTeleports = new HashMap<>();

    public void teleport(@NotNull final Player who, @NotNull final Location where) {
        final var task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            who.teleport(where);
            pendingTeleports.remove(who);
        }, 100L);
        final var previousTeleport = pendingTeleports.put(who, new PendingTeleport(where, task));
        if (previousTeleport != null) {
            previousTeleport.task.cancel();
            who.sendMessage("§5§lHomes §8| §cPrevious pending teleport aborted because a new teleport started.");
        }
    }

    @EventHandler
    private void OnPlayerMove(PlayerMoveEvent e) {
        // Apparently e.getTo() can be null, somehow.
        if (e.getTo() == null || e.getFrom().toVector() != e.getTo().toVector()) {
            final var player = e.getPlayer();
            final var pending = pendingTeleports.remove(player);
            if (pending != null) {
                pending.task.cancel();
                player.sendMessage("§5§lHomes §8| §cPending teleport aborted because you moved.");
            }
        }
    }

    @EventHandler
    private void OnPlayerQuit(PlayerQuitEvent e) {
        final var player = e.getPlayer();
        final var pending = pendingTeleports.remove(player);
        pending.task.cancel();
    }

    private record PendingTeleport(@NotNull Location location, @NotNull BukkitTask task) { }

}
