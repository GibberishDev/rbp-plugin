package com.gibbdev.recipebookplus.recipe.sync;

import com.gibbdev.recipebookplus.RecipeBookPlus;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.item.crafting.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

// ########################################
// RecipeSync is slightly modified version of JEIRecipeBridge plugin by Mrbysco
// https://github.com/Mrbysco/JEIRecipeBridge/tree/26.3 (c) 2026 Mrbysco - MIT license
// ########################################


public class RecipeSync implements Listener {

    @EventHandler
    public void recipebookplus$onReload(ServerResourcesReloadedEvent event) {
        MinecraftServer server = ((CraftServer) RecipeBookPlus.Plugin.getServer()).getServer();
        RecipeMap rm = server.getRecipeManager().recipes;
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        for (Player player : Bukkit.getOnlinePlayers()) {
            String brand = player.getClientBrandName();
            ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
            if (brand.equalsIgnoreCase("neoforge")) {
                sendNeoforgePayload(serverPlayer, server, rm, buffer);
            } else if (brand.equalsIgnoreCase("fabric")) {
                sendFabricPayload(serverPlayer, rm, buffer);
            }
        }
    }

    @EventHandler
    public void recipebookplus$onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        String brand = player.getClientBrandName();
        if (brand == null) return;
        if (!brand.equalsIgnoreCase("neoforge") && !brand.equalsIgnoreCase("fabric")) return;

        final ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        final MinecraftServer server = serverPlayer.level().getServer();
        final RecipeManager rm = server.getRecipeManager();
        RecipeMap recipeMap = rm.recipes;
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        if (brand.equalsIgnoreCase("neoforge")) {
            sendNeoforgePayload(serverPlayer, server, recipeMap, buffer);
        } else if (brand.equalsIgnoreCase("fabric")) {
            sendFabricPayload(serverPlayer, recipeMap, buffer);
        }

    }


    private static void sendPayload(ServerPlayer player, Identifier id, byte[] bytes) {
        player.connection.send(new ClientboundCustomPayloadPacket(new DiscardedPayload(id, bytes)));
    }

    private static void sendNeoforgePayload(ServerPlayer player, MinecraftServer server, RecipeMap recipeMap, RegistryFriendlyByteBuf buffer) {
        List<RecipeType<?>> allRecipeTypes = BuiltInRegistries.RECIPE_TYPE.stream().toList();
        var payload = NeoForgePayload.create(allRecipeTypes, recipeMap);
        NeoForgePayload.STREAM_CODEC.encode(buffer, payload);

        byte[] bytes = new byte[buffer.writerIndex()];
        buffer.getBytes(0, bytes);

        sendPayload(player, Identifier.fromNamespaceAndPath("neoforge", "recipe_content"), bytes);

        player.connection.send(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(server.registries())));
    }

    private static void sendFabricPayload(ServerPlayer player, RecipeMap recipeMap, RegistryFriendlyByteBuf buffer) {
        var list = new ArrayList<FabricPayload.Entry>();
        var seen = new HashSet<RecipeSerializer<?>>();

        for (RecipeSerializer<?> serializer : BuiltInRegistries.RECIPE_SERIALIZER) {
            if (!seen.add(serializer)) continue;
            List<RecipeHolder<?>> recipes = new ArrayList<>(recipeMap.values());
            if (!recipes.isEmpty()) {
                RecipeSerializer<?> entrySerializer = recipes.getFirst().value().getSerializer();
                list.add(new FabricPayload.Entry(entrySerializer, recipes));
            }
        }
        var payload = new FabricPayload(list);
        FabricPayload.CODEC.encode(buffer, payload);
        byte[] bytes = new byte[buffer.writerIndex()];
        buffer.getBytes(0, bytes);
        sendPayload(player, Identifier.fromNamespaceAndPath("fabric", "recipe_sync"), bytes);
    }

}
