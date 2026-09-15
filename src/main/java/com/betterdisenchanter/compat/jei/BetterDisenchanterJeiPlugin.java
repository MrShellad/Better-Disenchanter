package com.betterdisenchanter.compat.jei;

import com.betterdisenchanter.BetterDisenchanter;
import com.betterdisenchanter.BetterDisenchanterConfig;
import com.betterdisenchanter.recipe.CatalystRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class BetterDisenchanterJeiPlugin implements IModPlugin {

    public static final IRecipeType<CatalystRecipe> CATALYST_RECIPE_TYPE =
            IRecipeType.create(BetterDisenchanter.MOD_ID, "catalyst", CatalystRecipe.class);

    @Override
    public Identifier getPluginUid() {
        return BetterDisenchanter.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CatalystRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<CatalystRecipe> recipes = new ArrayList<>();

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            RecipeManager recipeManager = mc.getSingleplayerServer().getRecipeManager();
            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                if (holder.value() instanceof CatalystRecipe r && isRecipeEnabled(r)) {
                    recipes.add(r);
                }
            }
        }

        if (recipes.isEmpty()) {
            recipes.addAll(CatalystRecipe.getDefaultRecipes());
        }

        if (BetterDisenchanterConfig.isNoCatalystAllowed() && !recipes.contains(CatalystRecipe.DEFAULT_PROCESSOR)) {
            recipes.add(CatalystRecipe.DEFAULT_PROCESSOR);
        }

        registration.addRecipes(CATALYST_RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(CATALYST_RECIPE_TYPE, new ItemStack(BetterDisenchanter.DISENCHANTER_ITEM.get()));
    }

    private static boolean isRecipeEnabled(CatalystRecipe recipe) {
        if (recipe.ingredient.isEmpty()) {
            return BetterDisenchanterConfig.isNoCatalystAllowed();
        }
        return recipe.ingredient.get().items().anyMatch(h -> BetterDisenchanterConfig.isCatalystEnabled(h.value()));
    }
}
