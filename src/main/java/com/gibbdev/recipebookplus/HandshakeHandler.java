package com.gibbdev.recipebookplus;

import com.gibbdev.recipebookplus.config.ConfigClientSync;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.slf4j.MarkerFactory;

public class HandshakeHandler {

    public static void onHandshake(String ignoredChannel, Player player, byte[] ignoredData) {
        RecipeBookPlus.LOGGER.info(MarkerFactory.getMarker("ClientHandshake"),"Received handshake payload from: {}({})", player.getName(),player.identity().uuid());
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        MinecraftServer server = serverPlayer.level().getServer();
        HandshakeResponsePayload pl = HandshakeResponsePayload.create();
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        HandshakeResponsePayload.CODEC.encode(buffer, pl);
        byte[] bytes = new byte[buffer.writerIndex()];
        buffer.getBytes(0, bytes);
        serverPlayer.connection.send(new ClientboundCustomPayloadPacket(new DiscardedPayload(Identifier.fromNamespaceAndPath("recipebookplus","server_handshake_response"), bytes)));
        ConfigClientSync.sendConfigToPlayer(player);
    }

    public record HandshakeResponsePayload(String version, String serverModType) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HandshakeResponsePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("recipebookplus","server_handshake_response"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HandshakeResponsePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, HandshakeResponsePayload::version,
                ByteBufCodecs.STRING_UTF8, HandshakeResponsePayload::serverModType,
                HandshakeResponsePayload::new
        );

        public static HandshakeResponsePayload create() {
            return new HandshakeResponsePayload(
                    RecipeBookPlus.Plugin.getPluginMeta().getVersion(),
                    "PLUGIN"
            );
        }
        @Override
        public @NonNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }


}
