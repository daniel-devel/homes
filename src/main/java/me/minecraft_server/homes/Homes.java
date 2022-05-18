package me.minecraft_server.homes;

import me.minecraft_server.homes.commands.*;
import me.minecraft_server.homes.services.HomesService;
import me.minecraft_server.homes.services.InvitationService;
import me.minecraft_server.homes.services.TeleportService;
import me.minecraft_server.homes.util.AsyncExecutor;
import me.minecraft_server.homes.util.SyncExecutor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Homes extends JavaPlugin {

    private final AsyncExecutor asyncExecutor = new AsyncExecutor(this);
    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }

    private final SyncExecutor syncExecutor = new SyncExecutor(this);
    public SyncExecutor getSyncExecutor() {
        return syncExecutor;
    }

    private HomesService homesService = null;
    private void createHomesService() {
        homesService = new HomesService(this);
        getServer().getServicesManager().register(HomesService.class, homesService, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(homesService, this);
    }

    public HomesService getHomesService() {
        return homesService;
    }

    private TeleportService teleportService = null;
    private void createTeleportService() {
        teleportService = new TeleportService(this);
        getServer().getServicesManager().register(TeleportService.class, teleportService, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(teleportService, this);
    }

    public TeleportService getTeleportService() {
        return teleportService;
    }

    private InvitationService invitationService = null;
    private void createInvitationService() {
        invitationService = new InvitationService(this);
        getServer().getServicesManager().register(InvitationService.class, invitationService, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(invitationService, this);
    }

    public InvitationService getInvitationService() {
        return invitationService;
    }

    private void loadCommands() {
        loadCommand("home", new HomeCommand(this));
        loadCommand("sethome", new SetHomeCommand(this));
        loadCommand("delhome", new DelHomeCommand(this));
        loadCommand("invhome", new InvHomeCommand(this));
        loadCommand("acpthome", new AcptHomeCommand(this));
        loadCommand("declhome", new DeclHomeCommand(this));
    }

    private void loadCommand(@NotNull String name, Object command) {
        final var pluginCommand = Objects.requireNonNull(getCommand(name), name + " was not registered in plugin.yml!");
        pluginCommand.setExecutor((CommandExecutor) command);
        pluginCommand.setTabCompleter((TabCompleter) command);
    }

    @Override
    public void onEnable() {
        reloadConfig();
        createHomesService();
        createTeleportService();
        createInvitationService();
        loadCommands();
    }

    @Override
    public void onDisable() {
        homesService.shutdown();
    }

}
