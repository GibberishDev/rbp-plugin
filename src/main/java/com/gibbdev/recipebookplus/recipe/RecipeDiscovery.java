package com.gibbdev.recipebookplus.recipe;

import com.gibbdev.recipebookplus.RecipeBookPlus;
import com.gibbdev.recipebookplus.config.Config;
import com.google.gson.*;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class RecipeDiscovery implements Listener {

    private static final Map<Item, List<String>> recipeResultLookupMap = new HashMap<>();
    private static final Map<Item, List<String>> recipeIngredientLookupMap = new HashMap<>();

    private static final Map<UUID, Map<String, Set<String>>> playerData = new HashMap<>();
    private static boolean trackerDirty = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Boolean> playerDataLoadedFromDisk = new HashMap<>();

    @EventHandler
    public static void recipebookplus$onInventoryChange(PlayerInventorySlotChangeEvent event) {
        ItemStack newItem = getMCItemStack(event.getNewItemStack());
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!playerData.containsKey(uuid)) {
            playerData.put(uuid, new HashMap<>());
        }
        if (!playerData.get(uuid).containsKey("items")) resetPlayerCachedInventory(uuid);
        if (newItem.getItem() == Items.AIR) return;
        String id = BuiltInRegistries.ITEM.getKey(newItem.getItem()).toString();
        if (!playerDataLoadedFromDisk.containsKey(uuid) || !playerDataLoadedFromDisk.get(uuid)) {
            if (!playerData.get(uuid).containsKey("login_item_queue")) playerData.get(uuid).put("login_item_queue", new HashSet<>());
            playerData.get(uuid).get("login_item_queue").add(id);
        } else {
            RecipeBookPlus.LOGGER.info(playerData.get(uuid).getClass().toString());
            if (!playerData.get(uuid).get("items").contains(id)) {
                playerData.get(uuid).get("items").add(id);
                trackerDirty = true;
                checkRecipesToAward(uuid, newItem);
            }
        }
    }

    public static void checkRecipesToAward(UUID uuid, ItemStack item) {
        if (Config.RECIPE_DISCOVERY.get()) {
            if (Config.RECIPE_DISCOVERY_RESULT.get()) {
                if (recipeResultLookupMap.containsKey(item.getItem())) {
                    if (!playerData.get(uuid).containsKey("recipes")) playerData.get(uuid).put("recipes", new HashSet<>());
                    for (String holder : recipeResultLookupMap.get(item.getItem())) {
                        playerData.get(uuid).get("recipes").add(holder);
                    }
                }
            }
            if (Config.RECIPE_DISCOVERY_INGREDIENT.get()) {
                if (recipeIngredientLookupMap.containsKey(item.getItem())) {
                    if (!playerData.get(uuid).containsKey("recipes")) playerData.get(uuid).put("recipes", new HashSet<>());
                    for (String holder : recipeIngredientLookupMap.get(item.getItem())) {
                        playerData.get(uuid).get("recipes").add(holder);
                    }
                }
            }
        }
    }

    public static void resetPlayerCachedInventory(UUID uuid) {
        if (!playerData.containsKey(uuid)) {
            playerData.put(uuid, new HashMap<>());
        }
        playerData.get(uuid).put("items", new HashSet<>());
    }

    public static void rebuildRecipeLookupMaps() {
        List<RecipeHolder<?>> allRecipes =  ((CraftServer) RecipeBookPlus.Plugin.getServer()).getServer().getRecipeManager().recipes.values().stream().toList();
        recipeResultLookupMap.clear();
        recipeIngredientLookupMap.clear();
        for (RecipeHolder<?> holder : allRecipes) {
            ItemStack result = getMCItemStack(holder.toBukkitRecipe().getResult());
            if (!recipeResultLookupMap.containsKey(result.getItem())) recipeResultLookupMap.put(result.getItem(), new ArrayList<>());
            if (!recipeResultLookupMap.get(result.getItem()).contains(holder.id().identifier().toString())) recipeResultLookupMap.get(result.getItem()).add(holder.id().identifier().toString());
            List<Ingredient> ingredients = holder.value().placementInfo().ingredients();
            for (Ingredient ingredient : ingredients) {
                for (Holder<Item> ingredientHolder : ingredient.values) {
                    Item item = ingredientHolder.value();
                    if (!recipeIngredientLookupMap.containsKey(item)) recipeIngredientLookupMap.put(item, new ArrayList<>());
                    if (!recipeIngredientLookupMap.get(item).contains(holder.id().identifier().toString())) recipeIngredientLookupMap.get(item).add(holder.id().identifier().toString());
                }
            }
        }
        RecipeBookPlus.LOGGER.info("Rebuilt recipe discovery maps");
    }

    private static ItemStack getMCItemStack(org.bukkit.inventory.ItemStack bukkitStack){
        return CraftItemStack.asNMSCopy(bukkitStack);
    }

    //region FileIO
    public static void saveData() {
        if (!trackerDirty) return;
        trackerDirty = false;
        RecipeBookPlus.LOGGER.info(MarkerFactory.getMarker("RecipeDiscovery"),"Saving player data...");
        for (UUID uuid : playerData.keySet()) savePlayerData(uuid);
    }

    public static void savePlayerData(UUID uuid) {
        try {
            Path dir = RecipeBookPlus.Plugin.getDataFolder().toPath().resolve("recipe_discovery");
            Files.createDirectories(dir);
            playerData.get(uuid).remove("login_item_queue");
            String text = GSON.toJson(playerData.get(uuid));
            String filename = uuid.toString() + ".json";
            Path path = dir.resolve(filename);
            Files.writeString(path, text);
        } catch (IOException err) {
            RecipeBookPlus.LOGGER.error(MarkerFactory.getMarker("RecipeDiscovery"), "Error saving player data: {}",err.getMessage(),err);
        }
    }

    public static void loadPlayerData(UUID uuid) {
        try {
            Path dir = RecipeBookPlus.Plugin.getDataFolder().toPath().resolve("recipe_discovery");
            Files.createDirectories(dir);
            String filename = uuid.toString() + ".json";
            Path path = dir.resolve(filename);
            if (Files.exists(path)) {
                JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                if (!playerData.containsKey(uuid)) playerData.put(uuid, new HashMap<>());
                if (json.keySet().contains("items")) {
                    if (!playerData.get(uuid).containsKey("items")) playerData.get(uuid).put("items", new HashSet<>());
                    for (JsonElement element : json.getAsJsonArray("items")) {
                        playerData.get(uuid).get("items").add(element.getAsString());
                    }
                }
                if (json.keySet().contains("recipes")) {
                    if (!playerData.get(uuid).containsKey("recipes")) playerData.get(uuid).put("recipes", new HashSet<>());
                    for (JsonElement element : json.getAsJsonArray("recipes")) {
                        playerData.get(uuid).get("recipes").add(element.getAsString());
                    }
                }
                playerDataLoadedFromDisk.put(uuid, true);
                if (playerData.get(uuid).containsKey("login_item_queue")) {
                    if (!playerData.get(uuid).containsKey("items")) playerData.get(uuid).put("items", new HashSet<>());
                    Set<String> loginItems =  playerData.get(uuid).get("login_item_queue");
                    playerData.get(uuid).remove("login_item_queue");
                    for (String id : loginItems) {
                        if (!playerData.get(uuid).get("items").contains(id)) {
                            if (Material.matchMaterial(id) != null) {
                                playerData.get(uuid).get("items").add(id);
                                ItemStack item = getMCItemStack(new org.bukkit.inventory.ItemStack(Objects.requireNonNull(Material.matchMaterial(id))));
                                checkRecipesToAward(uuid, item);
                            }
                        }
                    }
                }
            }
        } catch (IOException err) {
            RecipeBookPlus.LOGGER.error(MarkerFactory.getMarker("RecipeDiscovery"), "Error loading player data: {}",err.getMessage(),err);
        }
    }

    @EventHandler
    public static void recipebookplus$onPlayerJoinEvent(PlayerJoinEvent event) {
        loadPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public static void recipebookplus$onPlayerQuitEvent(PlayerQuitEvent event) {
        savePlayerData(event.getPlayer().getUniqueId());
        playerData.remove(event.getPlayer().getUniqueId());
        playerDataLoadedFromDisk.remove(event.getPlayer().getUniqueId());
    }

    public static Map<UUID, String> getSavedPlayerNames() {
        Path saveDir = RecipeBookPlus.Plugin.getDataFolder().toPath().resolve("recipe_discovery");
        Map<UUID, String> output = new HashMap<>();
        try (Stream<Path> files = Files.list(saveDir)) {
            List<String> fileNames = files.map(path -> path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf("."))).toList();
            for (String name : fileNames) {
                UUID uuid = UUID.fromString(name);
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                output.put(uuid, player.getName());
            }
        } catch (IOException err) {
            RecipeBookPlus.LOGGER.error(MarkerFactory.getMarker("RecipeDiscovery"), "Error getting player names: {}",err.getMessage(),err);
        }
        return output;
    }


    //endregion
}
