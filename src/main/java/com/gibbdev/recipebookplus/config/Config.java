package com.gibbdev.recipebookplus.config;

import com.gibbdev.recipebookplus.RecipeBookPlus;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NonNull;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Config implements Listener {

    private static final Marker MARKER = MarkerFactory.getMarker("Config");
    public static final FileConfiguration CFG = RecipeBookPlus.Plugin.getConfig();

    public abstract static class ConfigEntry implements IConfigEntry {
        final String id;
        final Set<String> values;
        String value;
        final String description;
        final String defaultValue;
        ConfigEntry(String id, Set<String> values, String defaultValue, String description){
            this.id = id;
            this.values = values;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
            this.description = description;
        }
    }

    public interface IConfigEntry {
        void set(String newValue);
        String get();

    }

    public static class BooleanConfigEntry extends ConfigEntry {


        private static final List<String> true_strings = List.of(
                "1",
                "1b",
                "true",
                "t"
        );
        private static final List<String> false_strings = List.of(
                "0",
                "0b",
                "false",
                "f"
        );

        BooleanConfigEntry(String id, String defaultValue, String description) {
            super(id, Set.of("true","false"), defaultValue, description);
        }

        @Override
        public void set(String newValue) {
            String lcValue = newValue.toLowerCase(Locale.ROOT);
            if (true_strings.contains(lcValue)) {
                this.value = lcValue;
            } else if (false_strings.contains(lcValue)) {
                this.value = lcValue;
            } else {
                RecipeBookPlus.LOGGER.error(MARKER, "Could not set new value for {}. Invalid type. For this config entry valid values are: {}", this.id, this.values.toString());
            }
        }

        @Override
        public String get() {
            return Boolean.toString(CFG.getBoolean(this.id));
        }
    }


    @EventHandler
    public void recipebookplus$onReload(ServerResourcesReloadedEvent event) {
        RecipeBookPlus.Plugin.reloadConfig();
        syncConfigWithModClients();
    }

    public static void syncConfigWithModClients() {
        //TODO: sync config to clients
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendConfigToPlayer(player);
        }
    }

    public static void handleConfigRequest(String ignoredChannel, Player player, byte[] ignoredData) {
        sendConfigToPlayer(player);
    }

    public record ConfigPayload(byte[] ConfigData) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<ConfigPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("recipebookplus","config"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE_ARRAY, ConfigPayload::ConfigData,
                ConfigPayload::new
        );

        @Override
        public @NonNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static ConfigPayload create() {
            return new ConfigPayload(
                    getConfigJSON().toString().getBytes(StandardCharsets.UTF_8)
            );
        }
    }

    public static JsonObject getConfigJSON() {
        JsonObject json = new JsonObject();
        json.addProperty("recipe_discovery", getRecipeDiscovery());
        json.addProperty("recipe_discovery_result", getRecipeDiscoveryResult());
        json.addProperty("recipe_discovery_ingredient", getRecipeDiscoveryIngredient());
        json.addProperty("recipe_discovery_criterions", getRecipeDiscoveryCriterions());
        return json;
    }

    public static boolean getRecipeDiscovery() {
        return RecipeBookPlus.Plugin.getConfig().getBoolean("recipe_discovery");
    }

    public static boolean getRecipeDiscoveryResult() {
        return RecipeBookPlus.Plugin.getConfig().getBoolean("recipe_discovery_result");
    }

    public static boolean getRecipeDiscoveryIngredient() {
        return RecipeBookPlus.Plugin.getConfig().getBoolean("recipe_discovery_ingredient");
    }

    public static boolean getRecipeDiscoveryCriterions() {
        return RecipeBookPlus.Plugin.getConfig().getBoolean("recipe_discovery_criterions");
    }

    public static void sendConfigToPlayer(Player player) {
        //TODO: check if player has mod installed after handshake was complete and only send to those players
        ConfigPayload pl = ConfigPayload.create();
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RecipeBookPlus.SERVER.registryAccess());
        ConfigPayload.CODEC.encode(buffer, pl);
        byte[] bytes = new byte[buffer.writerIndex()];
        buffer.getBytes(0, bytes);
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundCustomPayloadPacket(new DiscardedPayload(Identifier.fromNamespaceAndPath("recipebookplus","config"), bytes)));
    }

    public static void setRecipeDiscovery(boolean value ) {
        RecipeBookPlus.Plugin.getConfig().set("recipe_discovery", value);
        RecipeBookPlus.Plugin.saveConfig();
        syncConfigWithModClients();
    }
    public static void setRecipeDiscoveryResult(boolean value ) {
        RecipeBookPlus.Plugin.getConfig().set("recipe_discovery_result", value);
        RecipeBookPlus.Plugin.saveConfig();
        syncConfigWithModClients();
    }
    public static void setRecipeDiscoveryIngredient(boolean value ) {
        RecipeBookPlus.Plugin.getConfig().set("recipe_discovery_ingredient", value);
        RecipeBookPlus.Plugin.saveConfig();
        syncConfigWithModClients();
    }
    public static void setRecipeDiscoveryCriterions(boolean value ) {
        RecipeBookPlus.Plugin.getConfig().set("recipe_discovery_criterions", value);
        RecipeBookPlus.Plugin.saveConfig();
        syncConfigWithModClients();
    }

}
