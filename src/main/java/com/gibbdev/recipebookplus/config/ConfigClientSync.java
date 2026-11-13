package com.gibbdev.recipebookplus.config;

import com.gibbdev.recipebookplus.RecipeBookPlus;
import com.gibbdev.recipebookplus.networking.CustomPayloads;
import com.gibbdev.recipebookplus.networking.CustomPayloads.ConfigPayload;
import com.google.gson.JsonObject;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static com.gibbdev.recipebookplus.config.Config.registeredConfigs;

public class ConfigClientSync implements Listener {

    @EventHandler
    public void recipebookplus$onReload(ServerResourcesReloadedEvent event) {
        RecipeBookPlus.PLUGIN.reloadConfig();
        syncConfigWithModClients();
    }
    public static void syncConfigWithModClients() {
        ConfigPayload payload = ConfigPayload.create();
        for (Player player : Bukkit.getOnlinePlayers()) {
            CustomPayloads.sendPayload(payload, player);
        }
    }
    public static JsonObject getConfigJSON() {
        JsonObject json = new JsonObject();
        registeredConfigs.forEach((id, entry)-> {
            if (entry.doSync()) {
                json.addProperty(id, entry.getString());
            }
        });
        return json;
    }
    public static void sendConfigToPlayer(Player player) {
        CustomPayloads.sendPayload(ConfigPayload.create(), player);
    }
}
