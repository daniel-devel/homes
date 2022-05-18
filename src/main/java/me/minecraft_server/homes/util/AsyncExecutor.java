package me.minecraft_server.homes.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public final class AsyncExecutor implements Executor {

    @NotNull
    private final JavaPlugin plugin;

    public AsyncExecutor(@NotNull final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull Runnable command) {
        if (Bukkit.isPrimaryThread())
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, command);
        else
            command.run();
    }

}
