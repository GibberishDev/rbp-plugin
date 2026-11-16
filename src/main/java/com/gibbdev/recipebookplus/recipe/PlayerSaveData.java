package com.gibbdev.recipebookplus.recipe;

import com.gibbdev.recipebookplus.RecipeBookPlus;
import com.gibbdev.recipebookplus.config.Config;
import com.gibbdev.recipebookplus.networking.HandshakeHandler;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PlayerSaveData implements Listener {

    public static final Map<UUID, PlayerSaveDataEntry> playerData = new HashMap<>();
    private static boolean isDirty = false;

    private static final Path SAVE_DIR = RecipeBookPlus.PLUGIN.getDataFolder().toPath().resolve("recipe_discovery");
    private static final String MARKER = "[RecipeBook+/PlayerSaveData]";
    public static final Pattern UUID_PATTERN = Pattern.compile("[\\da-fA-F]{8}(-[\\da-fA-F]{4}){3}-[\\da-fA-F]{12}");

    public class PlayerSaveDataEntry {
        private Set<String> knownRecipes = new HashSet<>();
        private int lastRecipeMapHash;
        private boolean isOnline;
        private boolean hasClientMod;
        private String lastKnownName;
        private Set<ItemStack> itemQueue;

        PlayerSaveDataEntry(UUID uuid) {

        }

        public boolean isRecipeKnown(RecipeHolder<?> holder) {
            return isRecipeKnown(holder.id().identifier().toString());
        }

        public boolean isRecipeKnown(String id) {
            return knownRecipes.contains(id);
        }

        public void setHasClientMod(boolean state) {
            this.hasClientMod = state;
        }

        public Set<ItemStack> popItemQueue() {
            Set<ItemStack> copy = Set.copyOf(this.itemQueue);
            this.itemQueue.clear();
            return copy;
        }
    }

    //region Listeners

    @EventHandler
    public static void recipebookplus$onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        //load data from disk if present, if not create new entry
        //also test for all items in inventory slots added during player setup as player join event fires after player connects. I don't want to use before join events as I don't want to deal with unloading data for players on failed connect
    }

    @EventHandler
    public static void recipebookplus$onPlayerQuit(PlayerQuitEvent event) {

    }

    @EventHandler
    public static void recipebookplus$onPlayerInventorySlotChange(PlayerInventorySlotChangeEvent event) {
        if (event.getNewItemStack().getType().equals(Material.AIR)) return;
        UUID uuid = event.getPlayer().getUniqueId();
        if (!Config.SAVE_DATA_FOR_ALL_PLAYERS.get() && HandshakeHandler.isHandshakeFailed(uuid)) return;
        playerTestItem(getMCItemStack(event.getNewItemStack()), uuid);
    }

    //endregion

    //region misc
    private static ItemStack getMCItemStack(org.bukkit.inventory.ItemStack bukkitStack){
        return CraftItemStack.asNMSCopy(bukkitStack);
    }

    public static boolean isValidUUID(String str) {
        return str!=null && UUID_PATTERN.matcher(str).matches();
    }
    //endregion

    //region event handlers and logic

    private static void playerTestItem(ItemStack item, UUID uuid) {

    }

    public static void onHandshakeFinish(UUID uuid, boolean success) {
        if (!success) {
            if (!Config.SAVE_DATA_FOR_ALL_PLAYERS.get()) {
                playerData.remove(uuid);
                return;
            }
        }
        if (playerData.containsKey(uuid)) {
            PlayerSaveDataEntry plr = playerData.get(uuid);
            plr.setHasClientMod(success);
            Set<ItemStack> itemQueueCopy = plr.popItemQueue();
            if (itemQueueCopy.isEmpty()) return;
            for (ItemStack item : itemQueueCopy) playerTestItem(item, uuid);
        }
    }

    //endregion


    //region File IO

    private static Set<UUID> getSavedPlayerIDs() {
        Set<UUID> output = new HashSet<>();
        try (Stream<Path> files = Files.list(SAVE_DIR)) {
            List<String> fileNames = files
                .map(p->p.getFileName().toString())
                .filter(n->n.endsWith(".json"))
                .map(n->n.substring(0,n.length()-5))
                .toList();
            fileNames.forEach(name->{if(isValidUUID(name))output.add(UUID.fromString(name));});
        } catch (IOException err) {
            RecipeBookPlus.LOGGER.error("{} Error reading player data: {}", MARKER,err.getMessage(),err);
        }
        return output;
    }

    private static boolean isSaveFilePresent(UUID uuid) {
        return Files.exists(SAVE_DIR.resolve(uuid.toString()+".json"));
    }

    private static void savePlayerData(UUID uuid) {

    }

    //endregion

}
