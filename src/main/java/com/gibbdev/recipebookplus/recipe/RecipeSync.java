package com.gibbdev.recipebookplus.recipe;

import com.gibbdev.recipebookplus.networking.CustomPayloads;
import com.gibbdev.recipebookplus.networking.CustomPayloads.NeoForgePayload;
import com.gibbdev.recipebookplus.networking.CustomPayloads.FabricPayload;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.item.crafting.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

// ########################################
// RecipeSync is modified version of JEIRecipeBridge plugin by Mrbysco
// https://github.com/Mrbysco/JEIRecipeBridge/tree/26.3 (c) 2026 Mrbysco - MIT license
// ########################################

public class RecipeSync implements Listener {

    @EventHandler
    public void recipebookplus$onReload(ServerResourcesReloadedEvent event) {
        Bukkit.getOnlinePlayers().forEach(RecipeSync::sendRecipesToPlayer);
    }
    @EventHandler
    public void recipebookplus$onPlayerJoin(PlayerJoinEvent event) {
        sendRecipesToPlayer(event.getPlayer());
    }

    private static void sendRecipesToPlayer(Player player) {
        String brand = player.getClientBrandName();
        if (brand==null||!List.of("neoforge","fabric").contains(brand))return;
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        final MinecraftServer server = serverPlayer.level().getServer();
        final RecipeMap recipeMap = server.getRecipeManager().recipes;
        if (brand.equals("neoforge")) {
            CustomPayloads.sendPayload(NeoForgePayload.create(BuiltInRegistries.RECIPE_TYPE.stream().toList(), recipeMap), player);
            serverPlayer.connection.send(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(server.registries())));
        } else {
            sendFabricPayload(player, recipeMap);
        }
    }

    private static void sendFabricPayload(Player player, RecipeMap recipeMap) {
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
        CustomPayloads.sendPayload(new FabricPayload(list), player);
    }

}
