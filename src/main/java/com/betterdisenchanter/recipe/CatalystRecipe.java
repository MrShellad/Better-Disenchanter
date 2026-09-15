package com.betterdisenchanter.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CatalystRecipe implements Recipe<CatalystRecipeInput> {

    public final Optional<Ingredient> ingredient;
    public final int count;
    public final String action;
    public final int maxEnchantments;
    public final int damageItem;
    public final int levelCost;
    public final boolean keepItem;
    public final boolean clearRemaining;
    public final String descriptionKey;

    public CatalystRecipe(Optional<Ingredient> ingredient, int count, String action, int maxEnchantments,
                          int damageItem, int levelCost, boolean keepItem, boolean clearRemaining,
                          String descriptionKey) {
        this.ingredient = ingredient;
        this.count = count;
        this.action = action;
        this.maxEnchantments = maxEnchantments;
        this.damageItem = damageItem;
        this.levelCost = levelCost;
        this.keepItem = keepItem;
        this.clearRemaining = clearRemaining;
        this.descriptionKey = descriptionKey;
    }

    public CatalystRecipe(Ingredient ingredient, int count, String action, int maxEnchantments,
                          int damageItem, int levelCost, boolean keepItem, boolean clearRemaining,
                          String descriptionKey) {
        this(Optional.of(ingredient), count, action, maxEnchantments, damageItem, levelCost, keepItem, clearRemaining, descriptionKey);
    }

    public static final CatalystRecipe DEFAULT_PROCESSOR = new CatalystRecipe(
            Optional.empty(), 0, "extract_first", 1, 0, 0, false, false, "betterdisenchanter.catalyst.none.desc"
    );

    public static List<CatalystRecipe> getDefaultRecipes() {
        List<CatalystRecipe> list = new ArrayList<>();
        if (com.betterdisenchanter.BetterDisenchanterConfig.isCatalystEnabled(Items.EMERALD)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.EMERALD), 1, "extract_random", 2, 0, 0, false, false, "betterdisenchanter.catalyst.emerald"));
        }
        if (com.betterdisenchanter.BetterDisenchanterConfig.isCatalystEnabled(Items.DIAMOND)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.DIAMOND), 1, "extract_first_plus_random", 2, 0, 0, false, false, "betterdisenchanter.catalyst.diamond"));
        }
        if (com.betterdisenchanter.BetterDisenchanterConfig.isCatalystEnabled(Items.ENDER_PEARL)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.ENDER_PEARL), 1, "extract_random", 1, 500, 0, true, false, "betterdisenchanter.catalyst.ender_pearl"));
        }
        if (com.betterdisenchanter.BetterDisenchanterConfig.isCatalystEnabled(Items.HEART_OF_THE_SEA)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.HEART_OF_THE_SEA), 1, "extract_all", 99, 0, 1, false, false, "betterdisenchanter.catalyst.heart_of_the_sea"));
        }
        if (com.betterdisenchanter.BetterDisenchanterConfig.isCatalystEnabled(Items.AMETHYST_SHARD)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.AMETHYST_SHARD), 1, "extract_first", 1, 0, 0, true, false, "betterdisenchanter.catalyst.amethyst_shard"));
        }
        if (com.betterdisenchanter.BetterDisenchanterConfig.isCatalystEnabled(Items.NETHER_STAR)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.NETHER_STAR), 1, "extract_all", 99, 0, 0, true, false, "betterdisenchanter.catalyst.nether_star"));
        }
        if (com.betterdisenchanter.BetterDisenchanterConfig.isCatalystEnabled(Items.EXPERIENCE_BOTTLE)) {
            list.add(new CatalystRecipe(Ingredient.of(Items.EXPERIENCE_BOTTLE), 1, "extract_highest", 99, 0, 0, false, false, "betterdisenchanter.catalyst.experience_bottle"));
        }
        return list;
    }

    public record DisenchantResult(ItemStack remainingInput, ItemStack outputBook) {}

    public DisenchantResult process(ItemStack inputItem, RandomSource random) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(inputItem);
        ItemStack outputBook = new ItemStack(Items.ENCHANTED_BOOK);

        if (enchantments.isEmpty()) {
            return new DisenchantResult(inputItem, outputBook);
        }

        List<Holder<Enchantment>> keys = new ArrayList<>(enchantments.keySet());
        List<Holder<Enchantment>> chosen = new ArrayList<>();

        switch (action) {
            case "extract_first_plus_random" -> {
                if (!keys.isEmpty()) {
                    chosen.add(keys.remove(0));
                }
                Collections.shuffle(keys);
                for (int i = 0; i < maxEnchantments && !keys.isEmpty(); i++) {
                    chosen.add(keys.remove(0));
                }
            }
            case "extract_all" -> {
                chosen.addAll(keys);
            }
            case "extract_first" -> {
                if (!keys.isEmpty()) {
                    chosen.add(keys.get(0));
                }
            }
            case "extract_highest" -> {
                int maxLevel = keys.stream().mapToInt(enchantments::getLevel).max().orElse(-1);
                for (Holder<Enchantment> h : keys) {
                    if (enchantments.getLevel(h) == maxLevel) {
                        chosen.add(h);
                    }
                }
            }
            case "extract_random" -> {
                Collections.shuffle(keys);
                for (int i = 0; i < maxEnchantments && !keys.isEmpty(); i++) {
                    chosen.add(keys.remove(0));
                }
            }
            default -> {
                if (!keys.isEmpty()) {
                    chosen.add(keys.get(0));
                }
            }
        }

        // Add to book
        for (Holder<Enchantment> h : chosen) {
            int level = Math.max(1, enchantments.getLevel(h) - levelCost);
            addEnchantmentToBook(outputBook, h, level);
        }

        // Process remaining input item
        ItemStack remaining = keepItem ? inputItem.copy() : ItemStack.EMPTY;
        if (!remaining.isEmpty()) {
            if (clearRemaining) {
                remaining.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            } else {
                EnchantmentHelper.updateEnchantments(remaining, mutable -> {
                    for (Holder<Enchantment> h : chosen) {
                        mutable.removeIf(entry -> entry.equals(h));
                    }
                });
            }

            if (damageItem > 0 && remaining.isDamageableItem()) {
                int newDamage = remaining.getDamageValue() + damageItem;
                if (newDamage >= remaining.getMaxDamage()) {
                    remaining = ItemStack.EMPTY;
                } else {
                    remaining.setDamageValue(newDamage);
                }
            }
        }

        return new DisenchantResult(remaining, outputBook);
    }

    public static void addEnchantmentToBook(ItemStack book, Holder<Enchantment> enchantment, int level) {
        ItemEnchantments current = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
        mutable.set(enchantment, level);
        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
    }

    public int getRequiredCount(ItemStack catalystStack) {
        if (catalystStack == null || catalystStack.isEmpty()) return this.count;
        return com.betterdisenchanter.BetterDisenchanterConfig.getCatalystRequiredCount(catalystStack.getItem(), this.count);
    }

    public boolean isEnabled(ItemStack catalystStack) {
        if (catalystStack == null || catalystStack.isEmpty()) {
            return com.betterdisenchanter.BetterDisenchanterConfig.isNoCatalystAllowed();
        }
        return com.betterdisenchanter.BetterDisenchanterConfig.isCatalystEnabled(catalystStack.getItem());
    }

    public boolean matchesIngredient(ItemStack stack) {
        if (!isEnabled(stack)) return false;
        return ingredient.map(ing -> ing.test(stack)).orElseGet(stack::isEmpty);
    }

    @Override
    public boolean matches(CatalystRecipeInput input, Level level) {
        ItemStack catalyst = input.catalyst();
        if (!isEnabled(catalyst)) return false;
        if (ingredient.isEmpty()) return catalyst.isEmpty();
        return ingredient.get().test(catalyst) && catalyst.getCount() >= getRequiredCount(catalyst);
    }

    @Override
    public ItemStack assemble(CatalystRecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<CatalystRecipe> getSerializer() {
        return ModRecipes.CATALYST_SERIALIZER.get();
    }

    @Override
    public RecipeType<CatalystRecipe> getType() {
        return ModRecipes.CATALYST_TYPE.get();
    }

    public static final MapCodec<CatalystRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.optionalFieldOf("ingredient").forGetter(r -> r.ingredient),
            Codec.INT.optionalFieldOf("count", 1).forGetter(r -> r.count),
            Codec.STRING.optionalFieldOf("action", "extract_random").forGetter(r -> r.action),
            Codec.INT.optionalFieldOf("max_enchantments", 1).forGetter(r -> r.maxEnchantments),
            Codec.INT.optionalFieldOf("damage_item", 0).forGetter(r -> r.damageItem),
            Codec.INT.optionalFieldOf("level_cost", 0).forGetter(r -> r.levelCost),
            Codec.BOOL.optionalFieldOf("keep_item", false).forGetter(r -> r.keepItem),
            Codec.BOOL.optionalFieldOf("clear_remaining", false).forGetter(r -> r.clearRemaining),
            Codec.STRING.optionalFieldOf("description", "").forGetter(r -> r.descriptionKey)
    ).apply(instance, CatalystRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CatalystRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CatalystRecipe decode(RegistryFriendlyByteBuf buffer) {
            Optional<Ingredient> ingredient = Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.decode(buffer);
            int count = buffer.readVarInt();
            String action = buffer.readUtf();
            int maxEnchantments = buffer.readVarInt();
            int damageItem = buffer.readVarInt();
            int levelCost = buffer.readVarInt();
            boolean keepItem = buffer.readBoolean();
            boolean clearRemaining = buffer.readBoolean();
            String descriptionKey = buffer.readUtf();
            return new CatalystRecipe(ingredient, count, action, maxEnchantments, damageItem, levelCost, keepItem, clearRemaining, descriptionKey);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, CatalystRecipe recipe) {
            Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
            buffer.writeVarInt(recipe.count);
            buffer.writeUtf(recipe.action);
            buffer.writeVarInt(recipe.maxEnchantments);
            buffer.writeVarInt(recipe.damageItem);
            buffer.writeVarInt(recipe.levelCost);
            buffer.writeBoolean(recipe.keepItem);
            buffer.writeBoolean(recipe.clearRemaining);
            buffer.writeUtf(recipe.descriptionKey);
        }
    };
}
