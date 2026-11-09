package com.gibbdev.recipebookplus.recipe.sync;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// ########################################
// RecipeSync is slightly modified version of JEIRecipeBridge plugin by Mrbysco
// https://github.com/Mrbysco/JEIRecipeBridge/tree/26.3 (c) 2026 Mrbysco - MIT license
// ########################################


public record NeoForgePayload(
        Set<RecipeType<?>> types,
        List<RecipeHolder<?>> holders
) implements CustomPacketPayload {

    public static final Type<CustomPacketPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("neoforge", "recipe_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.RECIPE_TYPE).apply(ByteBufCodecs.collection(HashSet::new)),
            NeoForgePayload::types,
            RecipeHolder.STREAM_CODEC.apply(ByteBufCodecs.list()),
            NeoForgePayload::holders,
            NeoForgePayload::new
    );

    public static NeoForgePayload create(Collection<RecipeType<?>> types, RecipeMap recipeMap) {
        var recipeTypeSet = Set.copyOf(types);
        // Fast-path for empty recipe type set (if no mod wants to sync anything)
        if (recipeTypeSet.isEmpty()) {
            return new NeoForgePayload(recipeTypeSet, List.of());
        } else {
            var recipeSubset = recipeMap.values().stream().filter(h -> recipeTypeSet.contains(h.value().getType())).toList();
            return new NeoForgePayload(recipeTypeSet, recipeSubset);
        }
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
