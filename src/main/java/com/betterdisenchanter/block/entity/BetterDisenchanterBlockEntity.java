package com.betterdisenchanter.block.entity;

import com.betterdisenchanter.BetterDisenchanter;
import com.betterdisenchanter.BetterDisenchanterConfig;
import com.betterdisenchanter.recipe.CatalystRecipe;
import com.betterdisenchanter.recipe.CatalystRecipeInput;
import com.betterdisenchanter.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BetterDisenchanterBlockEntity extends BlockEntity implements WorldlyContainer {

    public enum Status {
        PASSIVE,
        WAITING,
        ENCHANTING
    }

    private ItemStack book = ItemStack.EMPTY;
    private ItemStack item = ItemStack.EMPTY;
    private ItemStack activeCatalyst = ItemStack.EMPTY;

    private Status status = Status.PASSIVE;
    public float ticks = 0f;
    public float bookOpenAngle = 0f;
    public float bookLastOpenAngle = 0f;

    // Following player book rotation
    public float bookRot = 0f;
    public float bookLastRot = 0f;
    public float bookRotDir = 0f;
    public float bookRotForce = 0f;

    public BetterDisenchanterBlockEntity(BlockPos pos, BlockState state) {
        super(BetterDisenchanter.DISENCHANTER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BetterDisenchanterBlockEntity entity) {
        if (level.isClientSide) {
            entity.bookLastRot = entity.bookRot;

            Player player = level.getNearestPlayer((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, 3.0D, false);
            if (player != null) {
                double dx = player.getX() - ((double) pos.getX() + 0.5D);
                double dz = player.getZ() - ((double) pos.getZ() + 0.5D);
                entity.bookRotDir = (float) Mth.atan2(dz, dx);
            } else {
                entity.bookRotDir += 0.02F;
            }

            entity.bookRot = aroundRadial(entity.bookRot);
            entity.bookRotDir = aroundRadial(entity.bookRotDir);
            entity.bookRotForce = aroundRadial(entity.bookRotDir - entity.bookRot);
            entity.bookRot += entity.bookRotForce * 0.4F;

            entity.bookLastOpenAngle = entity.bookOpenAngle;
        }

        if (entity.status == Status.WAITING) {
            if (entity.item.isEmpty()) {
                if (entity.bookOpenAngle <= 0) {
                    if (!level.isClientSide) {
                        entity.status = Status.PASSIVE;
                        entity.bookOpenAngle = 0;
                        entity.notifyListeners();
                    }
                } else {
                    entity.bookOpenAngle = Math.max(0, entity.bookOpenAngle - 0.1f);
                }
            } else {
                if (entity.bookOpenAngle >= 1.0f) {
                    if (!level.isClientSide) {
                        entity.bookOpenAngle = 1.0f;
                        entity.notifyListeners();
                    }
                } else {
                    entity.bookOpenAngle = Math.min(1.0f, entity.bookOpenAngle + 0.1f);
                }
            }
        } else if (entity.status == Status.ENCHANTING) {
            if (level.isClientSide) {
                RandomSource random = level.random;
                for (int i = 0; i < 3; i++) {
                    double px = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.8D;
                    double py = pos.getY() + 1.1D + random.nextDouble() * 0.4D;
                    double pz = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.8D;
                    level.addParticle(ParticleTypes.ENCHANT, px, py, pz,
                            (random.nextDouble() - 0.5D) * 0.4D,
                            (random.nextDouble() - 0.5D) * 0.4D,
                            (random.nextDouble() - 0.5D) * 0.4D);
                }
            }

            if (entity.ticks > 60) {
                if (entity.bookOpenAngle > 0.0f) {
                    entity.bookOpenAngle = Math.max(0, entity.bookOpenAngle - 0.1f);
                } else {
                    if (!level.isClientSide) {
                        level.playSound(null, pos, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.BLOCKS, 0.8f, 0.8f);

                        CatalystRecipe recipe = entity.findMatchingCatalystRecipe(entity.activeCatalyst);
                        if (recipe == null) {
                            recipe = CatalystRecipe.DEFAULT_PROCESSOR;
                        }

                        CatalystRecipe.DisenchantResult result = recipe.process(entity.item, level.random);
                        entity.book = result.outputBook();
                        entity.item = result.remainingInput();
                        entity.activeCatalyst = ItemStack.EMPTY;
                        entity.status = Status.PASSIVE;
                        entity.ticks = 0;
                        entity.bookOpenAngle = 0;

                        entity.notifyListeners();
                    }
                }
            }
            entity.ticks += 1;
        }
    }

    public static float aroundRadial(float angle) {
        while (angle >= (float) Math.PI) {
            angle -= (float) (Math.PI * 2);
        }
        while (angle < -(float) Math.PI) {
            angle += (float) (Math.PI * 2);
        }
        return angle;
    }

    public static boolean hasEnchantments(ItemStack stack) {
        return !EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty();
    }

    @Nullable
    public CatalystRecipe findMatchingCatalystRecipe(ItemStack catalystStack) {
        if (level == null || catalystStack.isEmpty()) return null;
        if (!BetterDisenchanterConfig.isCatalystEnabled(catalystStack.getItem())) return null;

        List<RecipeHolder<CatalystRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.CATALYST_TYPE.get());
        for (RecipeHolder<CatalystRecipe> holder : recipes) {
            if (holder.value().matchesIngredient(catalystStack)) {
                return holder.value();
            }
        }
        return null;
    }

    public static void playCatalystStartSound(Level level, BlockPos pos, ItemStack catalyst) {
        if (catalyst.isEmpty()) {
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_FALL, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7f, 1.1f);
            return;
        }

        Item item = catalyst.getItem();
        if (item == Items.EMERALD) {
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_FALL, SoundSource.BLOCKS, 1.0f, 1.25f);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9f, 1.2f);
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.4f);
        } else if (item == Items.DIAMOND) {
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_FALL, SoundSource.BLOCKS, 1.0f, 1.45f);
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 0.9f, 1.3f);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8f, 1.4f);
        } else if (item == Items.ENDER_PEARL) {
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_FALL, SoundSource.BLOCKS, 1.0f, 0.75f);
            level.playSound(null, pos, SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 0.8f, 1.1f);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7f, 0.8f);
        } else if (item == Items.HEART_OF_THE_SEA) {
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_FALL, SoundSource.BLOCKS, 1.0f, 0.85f);
            level.playSound(null, pos, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 0.9f, 1.0f);
        } else if (item == Items.AMETHYST_SHARD) {
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_FALL, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.BLOCKS, 0.9f, 1.15f);
        } else if (item == Items.NETHER_STAR) {
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_FALL, SoundSource.BLOCKS, 1.0f, 0.6f);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.9f, 1.2f);
            level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8f, 1.4f);
        } else if (item == Items.EXPERIENCE_BOTTLE) {
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_FALL, SoundSource.BLOCKS, 1.0f, 1.35f);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.4f);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.4f);
        } else {
            float pitch = 0.8f + (Math.abs(item.hashCode()) % 40) * 0.01f;
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_FALL, SoundSource.BLOCKS, 1.0f, pitch);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, pitch);
        }
    }

    public ItemInteractionResult onUseItem(ItemStack handStack, Player player, InteractionHand hand) {
        if (level == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (status == Status.PASSIVE) {
            if (book.isEmpty()) {
                if (handStack.is(Items.BOOK)) {
                    if (!level.isClientSide) {
                        book = handStack.copyWithCount(1);
                        if (!player.isCreative()) handStack.shrink(1);
                        bookOpenAngle = 0;
                        level.playSound(null, worldPosition, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 0.8f, 0.8f);
                        notifyListeners();
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            } else if (book.is(Items.ENCHANTED_BOOK)) {
                if (!level.isClientSide) {
                    giveOrDrop(player, book);
                    book = ItemStack.EMPTY;
                    level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
                    notifyListeners();
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            } else if (!item.isEmpty() && !hasEnchantments(item)) {
                if (!level.isClientSide) {
                    giveOrDrop(player, item);
                    item = ItemStack.EMPTY;
                    level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
                    notifyListeners();
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            } else if (item.isEmpty() && book.is(Items.BOOK) && hasEnchantments(handStack)) {
                if (!level.isClientSide) {
                    item = handStack.copyWithCount(1);
                    if (!player.isCreative()) handStack.shrink(1);
                    status = Status.WAITING;
                    level.playSound(null, worldPosition, SoundEvents.CHISELED_BOOKSHELF_INSERT_ENCHANTED, SoundSource.BLOCKS, 0.8f, 1.2f);
                    notifyListeners();
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        } else if (status == Status.WAITING) {
            CatalystRecipe recipe = findMatchingCatalystRecipe(handStack);
            if (recipe != null) {
                int requiredCount = recipe.getRequiredCount(handStack);
                if (handStack.getCount() >= requiredCount) {
                    if (!level.isClientSide) {
                        activeCatalyst = handStack.copyWithCount(requiredCount);
                        if (!player.isCreative()) handStack.shrink(requiredCount);
                        status = Status.ENCHANTING;
                        ticks = 0;
                        playCatalystStartSound(level, worldPosition, activeCatalyst);
                        notifyListeners();
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                } else {
                    if (level.isClientSide) {
                        player.displayClientMessage(Component.translatable("text.betterdisenchanter.extra_catalysts_required", requiredCount - handStack.getCount()), true);
                    }
                    level.playSound(null, worldPosition, SoundEvents.CRAFTER_FAIL, SoundSource.BLOCKS, 0.8f, 2.0f);
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public InteractionResult onUseEmptyHand(Player player) {
        if (level == null) return InteractionResult.PASS;

        if (status == Status.PASSIVE) {
            if (!book.isEmpty()) {
                if (!level.isClientSide) {
                    giveOrDrop(player, book);
                    book = ItemStack.EMPTY;
                    level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
                    notifyListeners();
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else if (!item.isEmpty()) {
                if (!level.isClientSide) {
                    giveOrDrop(player, item);
                    item = ItemStack.EMPTY;
                    level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
                    notifyListeners();
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        } else if (status == Status.WAITING) {
            if (player.isShiftKeyDown()) {
                // Safely retrieve items
                if (!level.isClientSide) {
                    giveOrDrop(player, item);
                    item = ItemStack.EMPTY;
                    status = Status.PASSIVE;
                    level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
                    notifyListeners();
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                // Default disenchanting with no catalyst
                if (!BetterDisenchanterConfig.isNoCatalystAllowed()) {
                    return InteractionResult.PASS;
                }
                if (!level.isClientSide) {
                    activeCatalyst = ItemStack.EMPTY;
                    status = Status.ENCHANTING;
                    ticks = 0;
                    playCatalystStartSound(level, worldPosition, ItemStack.EMPTY);
                    notifyListeners();
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }

    public void onPunch(Player player) {
        if (level == null || level.isClientSide) return;

        if (status == Status.PASSIVE || status == Status.WAITING) {
            if (!item.isEmpty()) {
                giveOrDrop(player, item);
                item = ItemStack.EMPTY;
                status = Status.PASSIVE;
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
                notifyListeners();
            } else if (!book.isEmpty()) {
                giveOrDrop(player, book);
                book = ItemStack.EMPTY;
                status = Status.PASSIVE;
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
                notifyListeners();
            }
        }
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        if (!stack.isEmpty()) {
            if (!player.addItem(stack)) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 0.5, worldPosition.getZ(), stack);
            }
        }
    }

    public void notifyListeners() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    // NBT Serialization
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.book.isEmpty()) {
            tag.put("Book", this.book.save(registries));
        }
        if (!this.item.isEmpty()) {
            tag.put("Item", this.item.save(registries));
        }
        if (!this.activeCatalyst.isEmpty()) {
            tag.put("ActiveCatalyst", this.activeCatalyst.save(registries));
        }
        tag.putString("Status", this.status.name());
        tag.putFloat("Ticks", this.ticks);
        tag.putFloat("Angle", this.bookOpenAngle);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Book")) {
            this.book = ItemStack.parse(registries, tag.getCompound("Book")).orElse(ItemStack.EMPTY);
        } else {
            this.book = ItemStack.EMPTY;
        }

        if (tag.contains("Item")) {
            this.item = ItemStack.parse(registries, tag.getCompound("Item")).orElse(ItemStack.EMPTY);
        } else {
            this.item = ItemStack.EMPTY;
        }

        if (tag.contains("ActiveCatalyst")) {
            this.activeCatalyst = ItemStack.parse(registries, tag.getCompound("ActiveCatalyst")).orElse(ItemStack.EMPTY);
        } else {
            this.activeCatalyst = ItemStack.EMPTY;
        }

        if (tag.contains("Status")) {
            try {
                this.status = Status.valueOf(tag.getString("Status"));
            } catch (Exception ignored) {
                this.status = Status.PASSIVE;
            }
        }
        this.ticks = tag.getFloat("Ticks");
        this.bookOpenAngle = tag.getFloat("Angle");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // WorldlyContainer implementation for hopper automation
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP || side == Direction.DOWN) {
            return new int[]{0}; // Book slot
        }
        return new int[]{1}; // Item slot
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (status == Status.PASSIVE && stack.getCount() == 1) {
            if (slot == 0) {
                return book.isEmpty() && stack.is(Items.BOOK);
            } else if (slot == 1) {
                return !book.isEmpty() && item.isEmpty() && hasEnchantments(stack);
            }
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == 0) {
            return status == Status.PASSIVE && book.is(Items.ENCHANTED_BOOK);
        } else if (slot == 1) {
            return status == Status.PASSIVE && !item.isEmpty() && !hasEnchantments(item);
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return book.isEmpty() && item.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? book : item;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot).copy();
        stack.setCount(amount);
        getItem(slot).shrink(amount);
        notifyListeners();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        if (slot == 0) book = ItemStack.EMPTY;
        else item = ItemStack.EMPTY;
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            book = stack;
        } else if (slot == 1) {
            item = stack;
            if (!item.isEmpty() && book.is(Items.BOOK)) {
                status = Status.WAITING;
            }
        }
        notifyListeners();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        book = ItemStack.EMPTY;
        item = ItemStack.EMPTY;
        activeCatalyst = ItemStack.EMPTY;
        status = Status.PASSIVE;
        notifyListeners();
    }

    // Getters
    public ItemStack getBook() { return book; }
    public ItemStack getItem() { return item; }
    public ItemStack getActiveCatalyst() { return activeCatalyst; }
    public Status getStatus() { return status; }
}
