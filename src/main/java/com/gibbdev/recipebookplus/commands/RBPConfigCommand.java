package com.gibbdev.recipebookplus.commands;

import com.gibbdev.recipebookplus.RecipeBookPlus;
import com.gibbdev.recipebookplus.config.Config;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.permissions.Permission;

import java.util.Locale;

public class RBPConfigCommand{

    private static Component getAllConfigs() {
        MutableComponent text = Component.literal("RecipeBook+ Config:");
        Config.registeredConfigs.forEach((id, entry)->text.append(
                Component.literal("\n"+id+": ").append(entry.getChatValue()).withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal(entry.getDescription()))))
        ));
        return text;
    }

    public static void register(CommandDispatcher dispatcher) {
        dispatcher.register(Commands.literal("rbpconfig")
                .requires(src->src.getSender().hasPermission("recipebookplus:editconfig"))
                .executes(ctx->{
                    ctx.getSource().sendSuccess(
                            RBPConfigCommand::getAllConfigs,
                            false
                    );
                    return 1;
                })
                .then(
                        Commands.argument("config_name", StringArgumentType.word())
                        .suggests((_, builder) -> {
                            Config.registeredConfigs.keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes( ctx -> {
                            String config_name = StringArgumentType.getString(ctx, "config_name").toLowerCase(Locale.ROOT);
                            if (Config.registeredConfigs.containsKey(config_name)) {
                                ctx.getSource().sendSuccess(
                                        ()->Component.literal(config_name+": ").append(Config.registeredConfigs.get(config_name).getChatValue()),
                                        false
                                );
                                return 1;
                            } else {
                                ctx.getSource().sendFailure(
                                        Component.translatable("Invalid config key: \"%s\"", config_name)
                                );
                                return 0;
                            }
                        })
                        .then(
                                Commands.argument("config_value", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    String config_name = StringArgumentType.getString(ctx, "config_name").toLowerCase(Locale.ROOT);
                                    for (String value : Config.registeredConfigs.get(config_name).getValues()) builder.suggest(value);
                                    return builder.buildFuture();
                                })
                                .executes( ctx -> {
                                        String config_name = StringArgumentType.getString(ctx, "config_name");
                                        String config_value = StringArgumentType.getString(ctx, "config_value");
                                        Config.ConfigSetResponse resp = Config.setConfigValue(config_name, config_value);
                                        if (resp.success) {
                                            ctx.getSource().sendSuccess(
                                                    ()->resp.message,
                                                    false
                                            );
                                            return 1;
                                        } else {
                                            ctx.getSource().sendFailure(resp.message);
                                            return 0;
                                        }
                                })
                        )
                )
        );
    }
}
