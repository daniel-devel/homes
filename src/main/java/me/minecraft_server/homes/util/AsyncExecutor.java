package me.minecraft_server.homes.util;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

@RequiredArgsConstructor
public final class AsyncExecutor implements Executor {

    @NotNull
    private final JavaPlugin plugin;

    @Override
    public void execute(@NotNull Runnable command) {
        if (Bukkit.isPrimaryThread())
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, command);
        else
            command.run();
    }

}
