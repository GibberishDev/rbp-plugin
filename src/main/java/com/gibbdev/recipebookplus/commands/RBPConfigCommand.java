package com.gibbdev.recipebookplus.commands;

import com.gibbdev.recipebookplus.config.Config;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.Locale;

public class RBPConfigCommand{

    private static final List<String> config_names = List.of(
            "recipe_discovery",
            "recipe_discovery_result",
            "recipe_discovery_ingredient",
            "recipe_discovery_criterions"
    );

    public static void register(CommandDispatcher dispatcher) {
        dispatcher.register(Commands.literal("rbpconfig")
                .requires(src->src.getSender().hasPermission("recipebookplus:editconfig"))
                .executes(ctx->{
                    ctx.getSource().sendSuccess(
                            ()-> Component.literal("[RecipeBook+ config]")
                                    .withColor(TextColor.AQUA)
                                    .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal(
                                                "recipe_discovery: "+ (Config.getRecipeDiscovery()?"§2TRUE§r":"§4FALSE§r") + "\n"+
                                                    "recipe_discovery_result: "+ (Config.getRecipeDiscoveryResult()?"§2TRUE§r":"§4FALSE§r") + "\n"+
                                                    "recipe_discovery_ingredient: "+ (Config.getRecipeDiscoveryIngredient()?"§2TRUE§r":"§4FALSE§r") + "\n"+
                                                    "recipe_discovery_criterions: "+ (Config.getRecipeDiscoveryCriterions()?"§2TRUE§r":"§4FALSE§r") + "\n"
                                    )))),
                            false
                    );
                    return 1;
                })
                .then(
                        Commands.argument("config_name", StringArgumentType.word())
                        .suggests((_, builder) -> {
                            for (String cfg_id : config_names){
                                builder.suggest(cfg_id);
                            }
                            return builder.buildFuture();
                        })
                        .executes( ctx -> {
                            String config_name = StringArgumentType.getString(ctx, "config_name");
                            if (config_names.contains(config_name)) {
                                switch (config_name) {
                                    case "recipe_discovery" : {
                                        ctx.getSource().sendSuccess(
                                                ()->Component.literal("recipe_discovery: "+ (Config.getRecipeDiscovery()?"§2TRUE§r":"§4FALSE§r")),
                                                false
                                        );
                                        return 1;
                                    }
                                    case "recipe_discovery_result" : {
                                        ctx.getSource().sendSuccess(
                                                ()->Component.literal("recipe_discovery_result: "+ (Config.getRecipeDiscoveryResult()?"§2TRUE§r":"§4FALSE§r")),
                                                false
                                        );
                                        return 1;
                                    }
                                    case "recipe_discovery_ingredient" : {
                                        ctx.getSource().sendSuccess(
                                                ()->Component.literal("recipe_discovery_ingredient: "+ (Config.getRecipeDiscoveryIngredient()?"§2TRUE§r":"§4FALSE§r")),
                                                false
                                        );
                                        return 1;
                                    }
                                    case "recipe_discovery_criterions" : {
                                        ctx.getSource().sendSuccess(
                                                ()->Component.literal("recipe_discovery_criterions: "+ (Config.getRecipeDiscoveryCriterions()?"§2TRUE§r":"§4FALSE§r")),
                                                false
                                        );
                                        return 1;
                                    }
                                }
                            } else {
                                ctx.getSource().sendFailure(
                                        Component.literal("Invalid config key: \""+config_name+"\"\nValid entries are:\nrecipe_discovery, recipe_discovery_result, recipe_discovery_ingredient, recipe_discovery_criterions")
                                );
                                return 0;
                            }
                            return 1;
                        })
                        .then(
                                Commands.argument("config_value", StringArgumentType.word())
                                .suggests((_, builder) -> {
                                    builder.suggest("true");
                                    builder.suggest("false");
                                    return builder.buildFuture();
                                })
                                .executes( ctx -> {
                                        String config_value = StringArgumentType.getString(ctx, "config_value");
                                        String config_name = StringArgumentType.getString(ctx, "config_name");
                                        if (!true_strings.contains(config_value.toLowerCase(Locale.ROOT)) && !false_strings.contains(config_value.toLowerCase(Locale.ROOT))) {
                                            ctx.getSource().sendFailure(
                                                    Component.literal("Invalid config value: \""+config_value+"\"\nValid entries are:\ntrue, false")
                                            );
                                            return 0;
                                        } else {
                                            return switch (config_name) {
                                                case "recipe_discovery" -> {
                                                    Config.setRecipeDiscovery(true_strings.contains(config_value.toLowerCase(Locale.ROOT)));
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal("recipe_discovery is now: " + (Config.getRecipeDiscovery() ? "§2TRUE§r" : "§4FALSE§r")),
                                                            false
                                                    );
                                                    yield 1;
                                                }
                                                case "recipe_discovery_result" -> {
                                                    Config.setRecipeDiscoveryResult(true_strings.contains(config_value.toLowerCase(Locale.ROOT)));
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal("recipe_discovery_result is now: " + (Config.getRecipeDiscoveryResult() ? "§2TRUE§r" : "§4FALSE§r")),
                                                            false
                                                    );
                                                    yield 1;
                                                }
                                                case "recipe_discovery_ingredient" -> {
                                                    Config.setRecipeDiscoveryIngredient(true_strings.contains(config_value.toLowerCase(Locale.ROOT)));
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal("recipe_discovery_ingredient is now: " + (Config.getRecipeDiscoveryIngredient() ? "§2TRUE§r" : "§4FALSE§r")),
                                                            false
                                                    );
                                                    yield 1;
                                                }
                                                case "recipe_discovery_criterions" -> {
                                                    Config.setRecipeDiscoveryCriterions(true_strings.contains(config_value.toLowerCase(Locale.ROOT)));
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal("recipe_discovery_criterions is now: " + (Config.getRecipeDiscoveryCriterions() ? "§2TRUE§r" : "§4FALSE§r")),
                                                            false
                                                    );
                                                    yield 1;
                                                }
                                                default -> 1;
                                            };
                                        }
                                })
                        )
                )
        );
    }
}
