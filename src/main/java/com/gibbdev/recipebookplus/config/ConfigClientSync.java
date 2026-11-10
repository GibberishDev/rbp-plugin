package com.gibbdev.recipebookplus.config;

import com.gibbdev.recipebookplus.RecipeBookPlus;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;

import static com.gibbdev.recipebookplus.config.Config.registeredConfigs;

public class ConfigClientSync implements Listener {

    @EventHandler
    public void recipebookplus$onReload(ServerResourcesReloadedEvent event) {
        RecipeBookPlus.Plugin.reloadConfig();
        syncConfigWithModClients();
    }

    public static void syncConfigWithModClients() {
        //TODO: sync config to clients
        ClientboundCustomPayloadPacket payload = createPayload();
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendConfigToPlayer(player, payload);
        }
    }

    public static void handleConfigRequest(String ignoredChannel, Player player, byte[] ignoredData) {
        sendConfigToPlayer(player, createPayload());
    }

    private static ClientboundCustomPayloadPacket createPayload() {
        ConfigPayload pl = ConfigPayload.create();
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RecipeBookPlus.SERVER.registryAccess());
        ConfigPayload.CODEC.encode(buffer, pl);
        byte[] bytes = new byte[buffer.writerIndex()];
        buffer.getBytes(0, bytes);
        return new ClientboundCustomPayloadPacket(new DiscardedPayload(Identifier.fromNamespaceAndPath("recipebookplus","config"), bytes));
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
        registeredConfigs.forEach((id, entry)-> {
            if (entry.doSync) {
                json.addProperty(id, entry.getString());
            }
        });

        return json;
    }

    private static void sendConfigToPlayer(Player player, ClientboundCustomPayloadPacket payload) {
        ((CraftPlayer) player).getHandle().connection.send(payload);
    }

    public static void sendConfigToPlayer(Player player) {
        ((CraftPlayer) player).getHandle().connection.send(createPayload());
    }
}
