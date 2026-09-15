package com.betterdisenchanter.compat.jei;

import com.betterdisenchanter.BetterDisenchanter;
import com.betterdisenchanter.recipe.CatalystRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class CatalystRecipeCategory implements IRecipeCategory<CatalystRecipe> {

    private final IDrawable icon;
    private final IDrawableAnimated arrow;
    private final IDrawableStatic plusSign;

    private static final List<ItemStack> SAMPLE_INPUT_GEAR = new ArrayList<>();

    static {
        ItemStack[] items = new ItemStack[] {
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.DIAMOND_PICKAXE),
                new ItemStack(Items.DIAMOND_AXE),
                new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.BOW),
                new ItemStack(Items.CROSSBOW),
                new ItemStack(Items.TRIDENT),
                new ItemStack(Items.FISHING_ROD)
        };
        for (ItemStack is : items) {
            is.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            SAMPLE_INPUT_GEAR.add(is);
        }
    }

    public CatalystRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(BetterDisenchanter.DISENCHANTER_ITEM.get());
        this.arrow = guiHelper.createAnimatedRecipeArrow(30);
        this.plusSign = guiHelper.getRecipePlusSign();
    }

    @Override
    public IRecipeType<CatalystRecipe> getRecipeType() {
        return BetterDisenchanterJeiPlugin.CATALYST_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("betterdisenchanter.jei.catalyst_title");
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return 168;
    }

    @Override
    public int getHeight() {
        return 76;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CatalystRecipe recipe, IFocusGroup focuses) {
        // Slot 1: Catalyst
        IRecipeSlotBuilder catalystSlot = builder.addInputSlot(6, 6).setStandardSlotBackground();
        if (recipe.ingredient.isPresent()) {
            List<ItemStack> displayStacks = new ArrayList<>();
            recipe.ingredient.get().items().forEach(holder -> {
                ItemStack stack = new ItemStack(holder.value());
                stack.setCount(recipe.getRequiredCount(stack));
                displayStacks.add(stack);
            });
            catalystSlot.addItemStacks(displayStacks);
        } else {
            catalystSlot.addRichTooltipCallback((recipeSlotView, tooltip) -> {
                tooltip.add(Component.translatable("betterdisenchanter.catalyst.none.title"));
            });
        }

        // Slot 2: Enchanted gear
        IRecipeSlotBuilder gearSlot = builder.addInputSlot(39, 6).setStandardSlotBackground();
        gearSlot.addItemStacks(SAMPLE_INPUT_GEAR);

        // Slot 3: Book
        IRecipeSlotBuilder bookSlot = builder.addInputSlot(72, 6).setStandardSlotBackground();
        bookSlot.add(new ItemStack(Items.BOOK));

        // Output Slot 1: Enchanted Book
        IRecipeSlotBuilder bookOutputSlot = builder.addOutputSlot(120, 6).setStandardSlotBackground();
        bookOutputSlot.add(new ItemStack(Items.ENCHANTED_BOOK));

        // Output Slot 2: Preserved Gear (if applicable)
        if (recipe.keepItem) {
            IRecipeSlotBuilder gearOutputSlot = builder.addOutputSlot(144, 6).setStandardSlotBackground();
            gearOutputSlot.addItemStacks(SAMPLE_INPUT_GEAR);
            if (recipe.damageItem > 0) {
                gearOutputSlot.addRichTooltipCallback((recipeSlotView, tooltip) -> {
                    tooltip.add(Component.translatable("betterdisenchanter.catalyst.keep_with_damage", recipe.damageItem));
                });
            }
        }
    }

    @Override
    public void draw(CatalystRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 94, 7);
        plusSign.draw(guiGraphics, 26, 10);
        plusSign.draw(guiGraphics, 59, 10);

        Font font = Minecraft.getInstance().font;

        // Status badge / text
        Component status;
        int color;
        if (!recipe.keepItem) {
            status = Component.translatable("betterdisenchanter.catalyst.item_consumed");
            color = 0xFFD32F2F; // Red
        } else if (recipe.damageItem > 0) {
            status = Component.translatable("betterdisenchanter.catalyst.keep_with_damage", recipe.damageItem);
            color = 0xFFF57C00; // Orange
        } else {
            status = Component.translatable("betterdisenchanter.catalyst.keep_item");
            color = 0xFF388E3C; // Green
        }

        if (recipe.levelCost > 0) {
            Component penalty = Component.translatable("betterdisenchanter.catalyst.level_reduced", recipe.levelCost);
            status = Component.empty().append(status).append(Component.literal("  ")).append(penalty);
        }

        guiGraphics.text(font, status, 6, 30, color, false);

        // Description text
        Component desc = Component.translatable(recipe.descriptionKey);
        guiGraphics.textWithWordWrap(font, desc, 6, 43, 156, 0x444444);
    }

    @Override
    public Identifier getIdentifier(CatalystRecipe recipe) {
        if (recipe.descriptionKey != null && !recipe.descriptionKey.isEmpty()) {
            return BetterDisenchanter.id(recipe.descriptionKey.replace("betterdisenchanter.catalyst.", "catalyst_").replace('.', '_'));
        }
        return BetterDisenchanter.id("catalyst_default");
    }
}
