package me.minecraft_server.homes.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

public final class SyncExecutor implements Executor {

    @NotNull
    private final JavaPlugin plugin;

    public SyncExecutor(@NotNull final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull Runnable command) {
        if (Bukkit.isPrimaryThread())
            command.run();
        else
            plugin.getServer().getScheduler().runTask(plugin, command);
    }

}
