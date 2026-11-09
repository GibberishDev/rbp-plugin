package com.gibbdev.recipebookplus.config;

import com.gibbdev.recipebookplus.RecipeBookPlus;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;

public class Config {

    private static final Marker MARKER = MarkerFactory.getMarker("Config");
    public static Map<String, ConfigEntry> registeredConfigs = new HashMap<>();


    //region Config Entry Common

    public static class ConfigSetResponse {
        public final boolean success;
        public final Component message;
        ConfigSetResponse(boolean success, Component message) {
            this.success = success;
            this.message = message;
        }
    }

    public abstract static class ConfigEntry implements IConfigEntry {
        final String id;
        final Set<String> values;
        String value;
        final String description;
        final String defaultValue;
        ConfigEntry(String id, Set<String> values, String defaultValue, String description){
            this.id = id;
            this.values = values;
            this.defaultValue = defaultValue;
            this.description = description;
            registeredConfigs.put(this.id, this);
            setConfigValue(this.id,RecipeBookPlus.Plugin.getConfig().getString(this.id, defaultValue));
        }

        @Override
        public Set<String> getValues() {
            return this.values;
        }
    }

    public interface IConfigEntry {
        ConfigSetResponse set(String newValue);
        String get();
        Component getChatValue();
        String getDescription();
        Set<String> getValues();
    }

    public static String getConfigValue(String id) {
        if (registeredConfigs.containsKey(id)) {
            return registeredConfigs.get(id).value;
        } else {
            RecipeBookPlus.LOGGER.warn(MARKER, "Could not get value of \"{}\": config entry does not exist", id);
            return null;
        }
    }

    public static ConfigSetResponse setConfigValue(String id, String newValue) {
        if (registeredConfigs.containsKey(id)) {
            ConfigSetResponse response = registeredConfigs.get(id).set(newValue);
            if (response.success) {
                ConfigClientSync.syncConfigWithModClients();
            }
            return response;
        } else {
            RecipeBookPlus.LOGGER.warn(MARKER, "Could not set value of \"{}\": config entry does not exist", id);
            return new ConfigSetResponse(false, Component.translatable("Could not set value of \"%s\": config entry does not exist").withColor(TextColor.RED));
        }
    }

    //endregion

    //region Config Entry Types

    public static class BooleanConfigEntry extends ConfigEntry {
        private static final List<String> true_strings = List.of("1","1b","true","t");
        private static final List<String> false_strings = List.of("0","0b","false","f");
        BooleanConfigEntry(String id, boolean defaultValue, String description) {
            super(id, Set.of("true","false"), Boolean.toString(defaultValue).toLowerCase(Locale.ROOT), description);
        }

        @Override
        public ConfigSetResponse set(String newValue) {
            String lastValue = this.value;
            String lcValue = newValue.toLowerCase(Locale.ROOT);
            if (true_strings.contains(lcValue)) {
                this.value = "true";
            } else if (false_strings.contains(lcValue)) {
                this.value = "false";
            } else {
                RecipeBookPlus.LOGGER.error(MARKER, "Could not set \"{}\" for \"{}\". For this config entry valid values are: {}", lcValue, this.id, this.values.toString());
                return new ConfigSetResponse(false, Component.translatable("Could not set new value for \"%s\". For this config entry valid values are: %s", this.id, this.values.toString()).withColor(TextColor.RED));
            }
            RecipeBookPlus.Plugin.getConfig().set(id, Boolean.valueOf(this.value));
            RecipeBookPlus.Plugin.saveConfig();
            return new ConfigSetResponse(true, Component.translatable("Successfully changed value of \"%s\" to \"%s\"", this.id, this.value));
        }

        @Override
        public String get() {
            return Boolean.toString(RecipeBookPlus.Plugin.getConfig().getBoolean(this.id));
        }

        public boolean getBoolean() {
            return RecipeBookPlus.Plugin.getConfig().getBoolean(this.id);
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
    }

    //endregion

    public static void registerConfigEntries() {
        RecipeBookPlus.Plugin.reloadConfig();
        new BooleanConfigEntry (
                "recipe_discovery",
                true,
                "Whether to have all recipes discovered or let players learn recipes"
        );
        new BooleanConfigEntry(
                "recipe_discovery_result",
                true,
                "Recipe discovered and added to player knowledge book when they acquire result item\n(Ex: picking up a stick would grant player all recipes that have stick as a result)"
        );
        new BooleanConfigEntry(
                "recipe_discovery_ingredient",
                true,
                "Recipe discovered and added to player knowledge book when acquiring any ingredient used in the recipe\n(Ex: picking up a stick would grant player all recipes that have stick as an ingredient (Sword, torch, campfire, etc.)"
        );
        new BooleanConfigEntry(
                "recipe_discovery_criterions",
                true,
                "Recipe discovered and added to player knowledge book when certain criteria met. (VANILLA WAY)\n(Ex: advancement system based recipe discovery. Good example would be learning all boat recipes when touching water)"
        );
        RecipeBookPlus.Plugin.saveConfig();
    }

}
