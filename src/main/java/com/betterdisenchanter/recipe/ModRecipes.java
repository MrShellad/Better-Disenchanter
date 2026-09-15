package com.betterdisenchanter.recipe;

import com.betterdisenchanter.BetterDisenchanter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, BetterDisenchanter.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BetterDisenchanter.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CatalystRecipe>> CATALYST_TYPE =
            RECIPE_TYPES.register("catalyst", () -> RecipeType.simple(BetterDisenchanter.id("catalyst")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CatalystRecipe>> CATALYST_SERIALIZER =
            RECIPE_SERIALIZERS.register("catalyst", () -> new RecipeSerializer<>(CatalystRecipe.CODEC, CatalystRecipe.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
