package com.gibbdev.recipebookplus.networking;

import com.gibbdev.recipebookplus.RecipeBookPlus;
import com.gibbdev.recipebookplus.config.Config;
import com.gibbdev.recipebookplus.config.ConfigClientSync;
import com.gibbdev.recipebookplus.networking.CustomPayloads.HandshakeResponsePayload;
import com.gibbdev.recipebookplus.recipe.RecipeDiscovery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HandshakeHandler implements Listener {
    private static final Marker MARKER = MarkerFactory.getMarker("ClientHandshake");
    private enum HANDSHAKE_STATE {
        COMPLETED,
        FAILED,
        AWAITING
    }

    private static final Map<UUID, HANDSHAKE_STATE> handshakePerformed = new HashMap<>();

    public static void onHandshake(String ignoredChannel, Player player, byte[] ignoredData) {
        if (Config.VERBOSE_LOGGING.get()) RecipeBookPlus.LOGGER.info(MARKER,"Received handshake payload from: {}({})", player.getName(),player.identity().uuid());
        CustomPayloads.sendPayload(HandshakeResponsePayload.create(), player);
        ConfigClientSync.sendConfigToPlayer(player);
        handshakePerformed.put(player.getUniqueId(), HANDSHAKE_STATE.COMPLETED);
    }

    @EventHandler
    public static void recipebookplus$onPlayerJoinEvent(PlayerJoinEvent event) {
        handshakePerformed.put(event.getPlayer().getUniqueId(), HANDSHAKE_STATE.AWAITING);
        Bukkit.getScheduler().runTaskLaterAsynchronously(RecipeBookPlus.PLUGIN,()->onHandshakeTimeout(event.getPlayer().getUniqueId()),60L);
    }

    @EventHandler
    public static void recipebookplus$onPlayerQuitEvent(PlayerQuitEvent event) {
        handshakePerformed.remove(event.getPlayer().getUniqueId());
    }

    public static boolean isHandshakeComplete(UUID uuid) {
        return (handshakePerformed.get(uuid) == HANDSHAKE_STATE.COMPLETED);
    }

    public static boolean isHandshakeFailed(UUID uuid) {
        return (handshakePerformed.get(uuid) == HANDSHAKE_STATE.FAILED);
    }

    private static void onHandshakeTimeout(UUID uuid) {
        if (handshakePerformed.get(uuid) != HANDSHAKE_STATE.COMPLETED) {
            if (Config.VERBOSE_LOGGING.get()) {
                Player player = Bukkit.getPlayer(uuid);
                if (Config.VERBOSE_LOGGING.get() && player != null) RecipeBookPlus.LOGGER.info(MARKER,"{} did not complete handshake. Assuming RecipeBook+ mod not installed on client", player.getName());
            }
            handshakePerformed.put(uuid, HANDSHAKE_STATE.FAILED);
//            PlayerSaveData.handshakeFailed(uuid);
        }
    }


}
