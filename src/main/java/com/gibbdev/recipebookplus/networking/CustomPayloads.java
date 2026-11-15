package com.gibbdev.recipebookplus.networking;

import com.gibbdev.recipebookplus.RecipeBookPlus;
import com.gibbdev.recipebookplus.config.ConfigClientSync;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.SkipPacketDecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class CustomPayloads {

    public interface IPayloadEncoder{
        void encode(RegistryFriendlyByteBuf buffer, IPayloadEncoder payload);
    }

    // ########################################
    // NeoForge and Fabric payloads are slightly modified versions of JEIRecipeBridge plugin by Mrbysco
    // https://github.com/Mrbysco/JEIRecipeBridge/tree/26.3 (c) 2026 Mrbysco - MIT license
    // ########################################
    public record NeoForgePayload(Set<RecipeType<?>> types, List<RecipeHolder<?>> holders) implements CustomPacketPayload,IPayloadEncoder {
        public static final Type<CustomPacketPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("neoforge", "recipe_content"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.RECIPE_TYPE).apply(ByteBufCodecs.collection(HashSet::new)), NeoForgePayload::types,
                RecipeHolder.STREAM_CODEC.apply(ByteBufCodecs.list()), NeoForgePayload::holders,
                NeoForgePayload::new
        );
        public static NeoForgePayload create(Collection<RecipeType<?>> types, RecipeMap recipeMap) {
            var recipeTypeSet = Set.copyOf(types);
            if (recipeTypeSet.isEmpty()) {
                return new NeoForgePayload(recipeTypeSet, List.of());
            } else {
                var recipeSubset = recipeMap.values().stream().filter(h -> recipeTypeSet.contains(h.value().getType())).toList();
                return new NeoForgePayload(recipeTypeSet, recipeSubset);
            }
        }
        @Override public @NonNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, IPayloadEncoder payload) {
            CODEC.encode(buffer, (NeoForgePayload) payload);
        }
    }
    public record FabricPayload(List<Entry> entries) implements CustomPacketPayload,IPayloadEncoder {
        public static final Type<CustomPacketPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("fabric", "recipe_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FabricPayload> CODEC = Entry.CODEC.apply(ByteBufCodecs.list()).map(FabricPayload::new, FabricPayload::entries);
        public record Entry(RecipeSerializer<?> serializer, List<RecipeHolder<?>> holders) {
            public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.ofMember(Entry::write,Entry::read);
            private static Entry read(RegistryFriendlyByteBuf buf) {
                Identifier recipeId =  buf.readIdentifier();
                RecipeSerializer<?> recipeSerializer = BuiltInRegistries.RECIPE_SERIALIZER.getValue(recipeId);
                if (recipeSerializer==null) throw new SkipPacketDecoderException("Tried syncing unsupported packet serializer '" + recipeId + "'!");
                int count = buf.readVarInt();
                var list = new ArrayList<RecipeHolder<?>>();
                for (int i=0;i<count;i++) {
                    ResourceKey<Recipe<?>> id = buf.readResourceKey(Registries.RECIPE);
                    //noinspection deprecation
                    Recipe<?> recipe = recipeSerializer.streamCodec().decode(buf);
                    list.add(new RecipeHolder<>(id, recipe));
                }
                return new Entry(recipeSerializer, list);
            }
            private void write(RegistryFriendlyByteBuf buf) {
                buf.writeIdentifier(Objects.requireNonNull(BuiltInRegistries.RECIPE_SERIALIZER.getKey(this.serializer)));
                buf.writeVarInt(this.holders.size());
                //noinspection unchecked, deprecation
                StreamCodec<RegistryFriendlyByteBuf, Recipe<?>> serializer = ((StreamCodec<RegistryFriendlyByteBuf, Recipe<?>>) this.serializer.streamCodec());
                for (RecipeHolder<?> recipe : this.holders) {
                    buf.writeResourceKey(recipe.id());
                    serializer.encode(buf, recipe.value());
                }
            }
        }
        @Override public @NonNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, IPayloadEncoder payload) {
            CODEC.encode(buffer, (FabricPayload) payload);
        }
    }

    //region RB+ payloads

    public record RecipeDiscoveryPayload(byte[] recipeData) implements CustomPacketPayload,IPayloadEncoder {
        public static final CustomPacketPayload.Type<RecipeDiscoveryPayload> TYPE = new Type<>(RecipeBookPlus.id("discovery_data"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RecipeDiscoveryPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE_ARRAY, RecipeDiscoveryPayload::recipeData,
                RecipeDiscoveryPayload::new
        );
        public static RecipeDiscoveryPayload create(List<String> ids) {
            Map<String, Set<String>> data = new HashMap<>();
            data.put("recipes", new HashSet<>());
            data.get("recipes").addAll(ids);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return new RecipeDiscoveryPayload(gson.toJson(data).getBytes(StandardCharsets.UTF_8));
        }
        @Override public @NonNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, IPayloadEncoder payload) {
            CODEC.encode(buffer, (RecipeDiscoveryPayload) payload);
        }
    }
    public record NewRecipesPayload(byte[] recipeData) implements CustomPacketPayload,IPayloadEncoder {
        public static final CustomPacketPayload.Type<NewRecipesPayload> TYPE = new Type<>(RecipeBookPlus.id("new_recipes"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NewRecipesPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE_ARRAY, NewRecipesPayload::recipeData,
                NewRecipesPayload::new
        );
        public static NewRecipesPayload create(List<String> ids) {
            Map<String, Set<String>> data = new HashMap<>();
            data.put("new_recipes", new HashSet<>());
            data.get("new_recipes").addAll(ids);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return new NewRecipesPayload(gson.toJson(data).getBytes(StandardCharsets.UTF_8));
        }
        @Override public @NonNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, IPayloadEncoder payload) {
            CODEC.encode(buffer, (NewRecipesPayload) payload);
        }
    }
    public record RecipesResetPayload() implements CustomPacketPayload, IPayloadEncoder {
        public static final CustomPacketPayload.Type<RecipesResetPayload> TYPE = new Type<>(RecipeBookPlus.id("progress_reset"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RecipesResetPayload> CODEC = StreamCodec.unit(new RecipesResetPayload());
        @Override public @NonNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, IPayloadEncoder payload) {
            CODEC.encode(buffer, (RecipesResetPayload) payload);
        }
    }
    public record HandshakeResponsePayload(String version, String serverModType) implements CustomPacketPayload,IPayloadEncoder {
        public static final CustomPacketPayload.Type<HandshakeResponsePayload> TYPE = new Type<>(RecipeBookPlus.id("server_handshake_response"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HandshakeResponsePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, HandshakeResponsePayload::version,
            ByteBufCodecs.STRING_UTF8, HandshakeResponsePayload::serverModType,
            HandshakeResponsePayload::new
        );
        public static HandshakeResponsePayload create() {
            return new HandshakeResponsePayload(
                    RecipeBookPlus.PLUGIN.getPluginMeta().getVersion(),
                    "PLUGIN"
            );
        }
        @Override public @NonNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, IPayloadEncoder payload) {
            CODEC.encode(buffer, (HandshakeResponsePayload) payload);
        }
    }
    public record ConfigPayload(byte[] ConfigData) implements CustomPacketPayload,IPayloadEncoder {
        public static final CustomPacketPayload.Type<ConfigPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("recipebookplus","config"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE_ARRAY, ConfigPayload::ConfigData,
                ConfigPayload::new
        );
        public static ConfigPayload create() {
            return new ConfigPayload(
                    ConfigClientSync.getConfigJSON().toString().getBytes(StandardCharsets.UTF_8)
            );
        }
        @Override public @NonNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, IPayloadEncoder payload) {
            CODEC.encode(buffer, (ConfigPayload) payload);
        }
    }
    //endregion

    public static void sendPayload(CustomPacketPayload payload, UUID uuid) {
        if (Bukkit.getOnlinePlayers().stream().toList().stream().map(Entity::getUniqueId).toList().contains(uuid)) sendPayload(payload, Bukkit.getPlayer(uuid));
    }
    public static void sendPayload(CustomPacketPayload payload, Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        MinecraftServer server = serverPlayer.level().getServer();
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        ((IPayloadEncoder) payload).encode(buffer, (IPayloadEncoder) payload);
        byte[] bytes = new byte[buffer.writerIndex()];
        buffer.getBytes(0, bytes);
        serverPlayer.connection.send(new ClientboundCustomPayloadPacket(new DiscardedPayload(payload.type().id(),bytes)));
    }

}
