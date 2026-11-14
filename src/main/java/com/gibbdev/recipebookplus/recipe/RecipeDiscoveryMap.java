package com.gibbdev.recipebookplus.recipe;

import com.gibbdev.recipebookplus.RecipeBookPlus;
import com.gibbdev.recipebookplus.config.Config;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.*;

public class RecipeDiscoveryMap implements Listener {
    private static final Map<Item, Set<String>> recipeResultLookupMap = new HashMap<>();
    private static final Map<Item, Set<String>> recipeIngredientLookupMap = new HashMap<>();

    @EventHandler
    public static void recipebookplus$onServerResourcesReloaded(ServerResourcesReloadedEvent ignored) {
        rebuildRecipeMaps();
    }


    public static void rebuildRecipeMaps() {
//        Map<Item, Set<String>> oldRecipeResultLookupMap = Map.copyOf(recipeResultLookupMap);
//        Map<Item, Set<String>> oldIngredientResultLookupMap = Map.copyOf(ingredientResultLookupMap);
        RecipeMap rm = ((CraftServer) RecipeBookPlus.SERVER).getServer().getRecipeManager().recipes;
        recipeResultLookupMap.clear();
        recipeIngredientLookupMap.clear();
        for (RecipeHolder<?> holder : rm.values()) {
            Item result = CraftItemStack.asNMSCopy(holder.toBukkitRecipe().getResult()).getItem(); //I use bukkit here cus thats basically a rwrite
            if (!recipeResultLookupMap.containsKey(result)) recipeResultLookupMap.put(result, new HashSet<>());
            recipeResultLookupMap.get(result).add(holder.id().identifier().toString());
            List<Ingredient> ingredients = holder.value().placementInfo().ingredients();
            for (Ingredient ingredient : ingredients) {
                for (Holder<Item> ingredientHolder : ingredient.values) {
                    Item item = ingredientHolder.value();
                    if (!recipeIngredientLookupMap.containsKey(item)) recipeIngredientLookupMap.put(item, new HashSet<>());
                    recipeIngredientLookupMap.get(item).add(holder.id().identifier().toString());
                }
            }
        }
        //processDifferences()
    }

    public static Set<String> checkRecipes(Item item) {
        Set<String> output = new HashSet<>();
        if (Config.RECIPE_DISCOVERY.get()) {
            if (Config.RECIPE_DISCOVERY_RESULT.get()) {
                if (recipeResultLookupMap.containsKey(item)) output.addAll(recipeResultLookupMap.get(item));
            }
            if (Config.RECIPE_DISCOVERY_INGREDIENT.get()) {
                if (recipeIngredientLookupMap.containsKey(item)) output.addAll(recipeIngredientLookupMap.get(item));
            }
        }
        return output;
    }

    public static int getResultMapHash() {
        return recipeResultLookupMap.hashCode();
    }
    public static int getIngredientMapHash() {
        return recipeIngredientLookupMap.hashCode();
    }

}
