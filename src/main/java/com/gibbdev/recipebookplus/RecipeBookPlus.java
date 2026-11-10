package com.gibbdev.recipebookplus;

import com.gibbdev.recipebookplus.commands.RBPConfigCommand;
import com.gibbdev.recipebookplus.config.Config;
import com.gibbdev.recipebookplus.config.ConfigClientSync;
import com.gibbdev.recipebookplus.recipe.RecipeDiscovery;
import com.gibbdev.recipebookplus.recipe.sync.RecipeSync;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public final class RecipeBookPlus extends JavaPlugin {

    public static final Logger LOGGER = LoggerFactory.getLogger("RecipeBook+");
    public static Plugin Plugin;
    private static final Marker MARKER = MarkerFactory.getMarker("Main");
    public static final MinecraftServer SERVER = ((CraftServer) Bukkit.getServer()).getServer();

    @Override
    public void onEnable() {
        Plugin = this;
        getServer().getPluginManager().registerEvents(new RecipeSync(), this);
        getServer().getPluginManager().registerEvents(new ConfigClientSync(), this);
        getServer().getPluginManager().registerEvents(new RecipeDiscovery(), this);
        Config.registerConfigEntries();
        registerCommands();
        registerNetworking();
        getServer().getScheduler().runTaskTimerAsynchronously(
                this,
                RecipeDiscovery::saveData,
                6000L,
                6000L
        );
        RecipeDiscovery.rebuildRecipeLookupMaps();
    }

    @Override
    public void onDisable() {
        unregisterNetworking();
    }

    private static void registerCommands() {
        Plugin.getLifecycleManager().registerEventHandler( LifecycleEvents.COMMANDS, event -> RBPConfigCommand.register(event.registrar().getDispatcher()));
        LOGGER.info(MARKER, "Registered 1 commands");
    }

    private static void registerNetworking() {
        Plugin p = Plugin;
        final Server server = p.getServer();
        final Messenger messenger = server.getMessenger();
        messenger.registerOutgoingPluginChannel(p, "neoforge:recipe_content");
        messenger.registerOutgoingPluginChannel(p, "fabric:recipe_sync");
        messenger.registerOutgoingPluginChannel(p, "recipebookplus:config");
        messenger.registerOutgoingPluginChannel(p, "recipebookplus:server_handshake_response");
        messenger.registerIncomingPluginChannel(p, "recipebookplus:get_config", ConfigClientSync::handleConfigRequest);
        messenger.registerIncomingPluginChannel(p, "recipebookplus:client_handshake_request", HandshakeHandler::onHandshake);
        LOGGER.info(MARKER, "Registered 6 messenger channels");
    }

    private static void unregisterNetworking() {
        Plugin p = Plugin;
        final Server server = p.getServer();
        final Messenger messenger = server.getMessenger();
        messenger.unregisterOutgoingPluginChannel(p, "neoforge:recipe_content");
        messenger.unregisterOutgoingPluginChannel(p, "fabric:recipe_sync");
        messenger.unregisterOutgoingPluginChannel(p, "recipebookplus:config");
        messenger.unregisterOutgoingPluginChannel(p, "recipebookplus:server_handshake_response");
        messenger.unregisterIncomingPluginChannel(p, "recipebookplus:get_config", ConfigClientSync::handleConfigRequest);
        messenger.unregisterIncomingPluginChannel(p, "recipebookplus:client_handshake_request", HandshakeHandler::onHandshake);
        LOGGER.info(MARKER, "Unregistered 6 messenger channels");
    }

}
