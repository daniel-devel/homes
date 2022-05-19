package me.minecraft_server.homes;

import lombok.Getter;
import lombok.experimental.Accessors;
import me.minecraft_server.homes.commands.*;
import me.minecraft_server.homes.services.HomesService;
import me.minecraft_server.homes.services.InventoryService;
import me.minecraft_server.homes.services.InvitationService;
import me.minecraft_server.homes.services.TeleportService;
import me.minecraft_server.homes.util.AsyncExecutor;
import me.minecraft_server.homes.util.SyncExecutor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

@Accessors(prefix = "m")
public final class Homes extends JavaPlugin {

    @Getter
    private final AsyncExecutor mAsyncExecutor = new AsyncExecutor(this);

    @Getter
    private final SyncExecutor mSyncExecutor = new SyncExecutor(this);

    @Getter
    private HomesService mHomesService = null;

    @Getter
    private TeleportService mTeleportService = null;

    @Getter
    private InvitationService mInvitationService = null;

    @Getter
    private InventoryService mInventoryService = null;

    /**
     * Registers all commands to the classes.
     */
    private void loadCommands() {
        loadCommand("home", HomeCommand.class);
        loadCommand("homes", HomesCommand.class);
        loadCommand("sethome", SetHomeCommand.class);
        loadCommand("delhome", DelHomeCommand.class);
        loadCommand("invhome", InvHomeCommand.class);
        loadCommand("acpthome", AcptHomeCommand.class);
        loadCommand("declhome", DeclHomeCommand.class);
    }

    /**
     * Creates an instance of the command and registers it to the specified plugin command.
     * @param name The name of the command in the plugin.yml to register the command to.
     * @param clazz The type of the command class.
     * @param <T> The type of the command class.
     * @throws NullPointerException If the command is not registered in the plugin.yml file.
     */
    private <T extends CommandExecutor & TabCompleter> void loadCommand(@NotNull final String name, @NotNull final Class<T> clazz) {
        final var pluginCommand = Objects.requireNonNull(getCommand(name), name + " was not registered in plugin.yml!");
        final var instance = createInstance(clazz);
        pluginCommand.setExecutor(instance);
        pluginCommand.setTabCompleter(instance);
    }

    /**
     * Loads all services and sets them to the fields.
     */
    private void loadServices() {
        mHomesService = createService(HomesService.class);
        mTeleportService = createService(TeleportService.class);
        mInvitationService = createService(InvitationService.class);
        mInventoryService = createService(InventoryService.class);
    }

    /**
     * Creates a service and registers it to the bukkit service manager and
     * all events inside the service in bukkit plugin manager.
     * @param clazz The class to create an instance of register.
     * @return The created instance.
     * @param <T> The type of the service class.
     */
    private <T extends Listener> T createService(Class<T> clazz) {
        final var instance = createInstance(clazz);
        getServer().getServicesManager().register(clazz, instance, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(instance, this);
        return instance;
    }

    /**
     * Create instance, first by trying to access the constructor with a homes plugin parameter,
     * if it fails, with the default constructor as parameter.
     * @param clazz The class to create.
     * @return A new instance of that class.
     * @param <T> The type of the class.
     */
    private <T> T createInstance(Class<T> clazz) {
        try {
            try {
                return clazz.getConstructor(Homes.class).newInstance(this);
            } catch (NoSuchMethodException ignored) {
                return clazz.getConstructor().newInstance();
            }
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        reloadConfig();
        loadServices();
        loadCommands();
    }

    @Override
    public void onDisable() {
        mHomesService.shutdown();
    }

}
