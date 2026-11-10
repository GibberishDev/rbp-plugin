package com.gibbdev.recipebookplus.config;

import com.gibbdev.recipebookplus.RecipeBookPlus;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;


@SuppressWarnings("unused")
public class Config {

    private static final Marker MARKER = MarkerFactory.getMarker("Config");
    public static Map<String, ConfigEntry<?>> registeredConfigs = new HashMap<>();


    //region Config Entry Common

    public static class ConfigSetResponse {
        public final boolean success;
        public final Component message;
        ConfigSetResponse(boolean success, Component message) {
            this.success = success;
            this.message = message;
        }
    }

    public abstract static class ConfigEntry<T> implements IConfigEntry<T> {
        final String id;
        final Set<String> values;
        T value;
        final String description;
        final T defaultValue;
        final boolean doSync;
        ConfigEntry(String id, Set<String> chatValues, T defaultValue, boolean doSync, String description){
            this.id = id;
            this.values = chatValues;
            this.doSync = doSync;
            this.defaultValue = defaultValue;
            this.description = description;
            this.value = defaultValue;
        }

        public void register() {
            registeredConfigs.put(this.id, this);
            this.setString(RecipeBookPlus.Plugin.getConfig().getString(this.id));
        }

    }

    public interface IConfigEntry<T> {
        ConfigSetResponse setString(String newValue);
        String getString();
        Component getChatValue();
        String getDescription();
        Set<String> getChatValues();
        ConfigSetResponse set(T newValue);
        T get();
    }

    //endregion

    //region Config Entry Types

    public static class BooleanConfigEntry extends ConfigEntry<Boolean> {
        private static final List<String> true_strings = List.of("1","1b","true","t");
        private static final List<String> false_strings = List.of("0","0b","false","f");
        BooleanConfigEntry(String id, boolean defaultValue, boolean doSync, String description) {
            super(id, Set.of("true","false"), defaultValue, doSync, description);
        }

        @Override
        public ConfigSetResponse set(Boolean newValue) {
            if (this.value == newValue) return new ConfigSetResponse(true,Component.empty());
            this.value = newValue;
            RecipeBookPlus.Plugin.getConfig().set(id, newValue);
            RecipeBookPlus.Plugin.saveConfig();
            return new ConfigSetResponse(true,Component.empty());

        }

        @Override
        public ConfigSetResponse setString(String newValue) {
            String lcValue = newValue.toLowerCase(Locale.ROOT);
            boolean newBool;
            if (true_strings.contains(lcValue)) {
                newBool = true;
            } else if (false_strings.contains(lcValue)) {
                newBool = false;
            } else {
                RecipeBookPlus.LOGGER.error(MARKER, "Could not set \"{}\" for \"{}\". For this config entry valid values are: {}", lcValue, this.id, this.values.toString());
                return new ConfigSetResponse(false, Component.translatable("Could not set new value for \"%s\". For this config entry valid values are: %s", this.id, this.values.toString()).withColor(TextColor.RED));
            }
            if (this.value == newBool) return new ConfigSetResponse(true,Component.empty());
            this.value = newBool;
            RecipeBookPlus.Plugin.getConfig().set(id, this.value);
            RecipeBookPlus.Plugin.saveConfig();
            return new ConfigSetResponse(true, Component.translatable("Successfully changed value of \"%s\" to \"%s\"", this.id, this.value));
        }

        @Override
        public Boolean get() {
            return RecipeBookPlus.Plugin.getConfig().getBoolean(this.id);
        }

        public boolean getBoolean() {
            return RecipeBookPlus.Plugin.getConfig().getBoolean(this.id);
        }

        @Override
        public String getString() {
            return Boolean.toString(this.value);
        }

        @Override
        public Component getChatValue() {
            if (getBoolean()) {
                return Component.literal("True").withColor(TextColor.GREEN);
            } else {
                return Component.literal("False").withColor(TextColor.RED);
            }
        }

        @Override
        public String getDescription() {
            return this.description;
        }

        @Override
        public Set<String> getChatValues() {
            return this.values;
        }
    }

    //endregion

    //region registry

    public static final BooleanConfigEntry RECIPE_DISCOVERY = new BooleanConfigEntry (
            "recipe_discovery",
            true,
            true,
            "Whether to have all recipes discovered or let players learn recipes"
    );
    public static final BooleanConfigEntry RECIPE_DISCOVERY_RESULT = new BooleanConfigEntry (
            "recipe_discovery_result",
            true,
            true,
            "Recipe discovered and added to player knowledge book when they acquire result item\n(Ex: picking up a stick would grant player all recipes that have stick as a result)"
    );
    public static final BooleanConfigEntry RECIPE_DISCOVERY_INGREDIENT = new BooleanConfigEntry (
            "recipe_discovery_ingredient",
            true,
            true,
            "Recipe discovered and added to player knowledge book when acquiring any ingredient used in the recipe\n(Ex: picking up a stick would grant player all recipes that have stick as an ingredient (Sword, torch, campfire, etc.)"
    );
    public static final BooleanConfigEntry RECIPE_DISCOVERY_CRITERIONS = new BooleanConfigEntry (
            "recipe_discovery_criterions",
            true,
            true,
            "Recipe discovered and added to player knowledge book when certain criteria met. (VANILLA WAY)\n(Ex: advancement system based recipe discovery. Good example would be learning all boat recipes when touching water)"
    );


    public static void registerConfigEntries() {
        RecipeBookPlus.Plugin.reloadConfig();
        RECIPE_DISCOVERY.register();
        RECIPE_DISCOVERY_RESULT.register();
        RECIPE_DISCOVERY_INGREDIENT.register();
        RECIPE_DISCOVERY_CRITERIONS.register();
        RecipeBookPlus.Plugin.saveConfig();
    }

    //endregion

}
