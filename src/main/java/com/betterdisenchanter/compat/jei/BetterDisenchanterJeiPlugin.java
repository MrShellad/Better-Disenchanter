package com.betterdisenchanter.compat.jei;

import com.betterdisenchanter.BetterDisenchanter;
import com.betterdisenchanter.BetterDisenchanterConfig;
import com.betterdisenchanter.recipe.CatalystRecipe;
import com.betterdisenchanter.recipe.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class BetterDisenchanterJeiPlugin implements IModPlugin {

    public static final RecipeType<CatalystRecipe> CATALYST_RECIPE_TYPE =
            RecipeType.create(BetterDisenchanter.MOD_ID, "catalyst", CatalystRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
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
        RecipeManager recipeManager = null;
        if (mc.level != null) {
            recipeManager = mc.level.getRecipeManager();
        } else if (mc.getConnection() != null) {
            recipeManager = mc.getConnection().getRecipeManager();
        }

        if (recipeManager != null) {
            List<RecipeHolder<CatalystRecipe>> recipeHolders = recipeManager.getAllRecipesFor(ModRecipes.CATALYST_TYPE.get());
            for (RecipeHolder<CatalystRecipe> holder : recipeHolders) {
                CatalystRecipe r = holder.value();
                if (isRecipeEnabled(r)) {
                    recipes.add(r);
                }
            }
        }

        if (recipes.isEmpty()) {
            recipes.addAll(getDefaultRecipes());
        }

        if (BetterDisenchanterConfig.isNoCatalystAllowed() && !recipes.contains(CatalystRecipe.DEFAULT_PROCESSOR)) {
            recipes.add(CatalystRecipe.DEFAULT_PROCESSOR);
        }

        registration.addRecipes(CATALYST_RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(BetterDisenchanter.DISENCHANTER_ITEM.get()), CATALYST_RECIPE_TYPE);
    }

    private static boolean isRecipeEnabled(CatalystRecipe recipe) {
        if (recipe.ingredient.isEmpty()) {
            return BetterDisenchanterConfig.isNoCatalystAllowed();
        }
        for (ItemStack item : recipe.ingredient.getItems()) {
            if (BetterDisenchanterConfig.isCatalystEnabled(item.getItem())) {
                return true;
            }
        }
        return false;
    }

    private static List<CatalystRecipe> getDefaultRecipes() {
        List<CatalystRecipe> list = new ArrayList<>();
        if (BetterDisenchanterConfig.isCatalystEnabled(Items.EMERALD)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.EMERALD), 1, "extract_random", 2, 0, 0, false, false, "betterdisenchanter.catalyst.emerald"));
        }
        if (BetterDisenchanterConfig.isCatalystEnabled(Items.DIAMOND)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.DIAMOND), 1, "extract_first_plus_random", 2, 0, 0, false, false, "betterdisenchanter.catalyst.diamond"));
        }
        if (BetterDisenchanterConfig.isCatalystEnabled(Items.ENDER_PEARL)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.ENDER_PEARL), 1, "extract_random", 1, 500, 0, true, false, "betterdisenchanter.catalyst.ender_pearl"));
        }
        if (BetterDisenchanterConfig.isCatalystEnabled(Items.HEART_OF_THE_SEA)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.HEART_OF_THE_SEA), 1, "extract_all", 99, 0, 1, false, false, "betterdisenchanter.catalyst.heart_of_the_sea"));
        }
        if (BetterDisenchanterConfig.isCatalystEnabled(Items.AMETHYST_SHARD)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.AMETHYST_SHARD), 1, "extract_first", 1, 0, 0, true, false, "betterdisenchanter.catalyst.amethyst_shard"));
        }
        if (BetterDisenchanterConfig.isCatalystEnabled(Items.NETHER_STAR)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.NETHER_STAR), 1, "extract_all", 99, 0, 0, true, false, "betterdisenchanter.catalyst.nether_star"));
        }
        if (BetterDisenchanterConfig.isCatalystEnabled(Items.EXPERIENCE_BOTTLE)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.EXPERIENCE_BOTTLE), 1, "extract_highest", 99, 0, 0, false, false, "betterdisenchanter.catalyst.experience_bottle"));
        }
        return list;
    }
}
