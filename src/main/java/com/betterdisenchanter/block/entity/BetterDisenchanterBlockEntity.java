package com.betterdisenchanter.block.entity;

import com.betterdisenchanter.BetterDisenchanter;
import com.betterdisenchanter.BetterDisenchanterConfig;
import com.betterdisenchanter.recipe.CatalystRecipe;
import com.betterdisenchanter.recipe.CatalystRecipeInput;
import com.betterdisenchanter.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
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
import net.minecraft.world.item.enchantment.ItemEnchantments;
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

        // Smooth book open/close angle animation (driven independently on both client & server without packet spam)
        boolean shouldBookOpen = (entity.status == Status.WAITING || entity.status == Status.ENCHANTING) && !entity.item.isEmpty();
        if (shouldBookOpen) {
            entity.bookOpenAngle = Math.min(1.0f, entity.bookOpenAngle + 0.1f);
        } else {
            entity.bookOpenAngle = Math.max(0.0f, entity.bookOpenAngle - 0.1f);
        }

        // Auto-correct status if items were removed externally (e.g. hoppers)
        if (entity.status == Status.WAITING && (entity.item.isEmpty() || entity.book.isEmpty())) {
            entity.status = Status.PASSIVE;
            if (!level.isClientSide) {
                entity.notifyListeners();
            }
        }

        if (entity.status == Status.ENCHANTING) {
            if (level.isClientSide) {
                if (BetterDisenchanterConfig.isMagicCircleEnabled()) {
                    spawnRitualParticles(level, pos, entity);
                }
            } else {
                // 仪式中间段递进共鸣音效 (Buildup chimes)
                if (entity.ticks == 10) {
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7f, 1.1f);
                } else if (entity.ticks == 20) {
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9f, 1.35f);
                    level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_STEP, SoundSource.BLOCKS, 0.6f, 1.3f);
                }
            }

            // 紧凑 30 ticks (1.5秒) 瞬间完成仪式
            if (entity.ticks >= 30) {
                if (level.isClientSide) {
                    if (BetterDisenchanterConfig.isCompletionBurstEnabled()) {
                        spawnCompletionRing(level, pos, entity.activeCatalyst);
                    }
                } else {
                    CatalystRecipe recipe = entity.findMatchingCatalystRecipe(entity.activeCatalyst);
                    if (recipe == null) {
                        recipe = CatalystRecipe.DEFAULT_PROCESSOR;
                    }

                    playCompletionSounds(level, pos, entity.activeCatalyst, recipe.keepItem);

                    CatalystRecipe.DisenchantResult result = recipe.process(entity.item, level.random);
                    entity.book = result.outputBook();
                    entity.item = result.remainingInput();
                    entity.activeCatalyst = ItemStack.EMPTY;
                    entity.status = Status.PASSIVE;
                    entity.ticks = 0;
                    entity.bookOpenAngle = 0;
                    entity.bookLastOpenAngle = 0;

                    entity.notifyListeners();
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
        if (stack == null || stack.isEmpty()) return false;
        if (stack.isEnchanted()) return true;
        if (!EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty()) return true;
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) return true;
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        return enchants != null && !enchants.isEmpty();
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

    public static DustParticleOptions getThemedDust(ItemStack catalyst, float scale) {
        if (catalyst.isEmpty()) {
            return new DustParticleOptions(new Vector3f(0.70f, 0.50f, 0.95f), scale);
        }
        Item item = catalyst.getItem();
        if (item == Items.EMERALD) {
            return new DustParticleOptions(new Vector3f(0.15f, 0.95f, 0.35f), scale);
        } else if (item == Items.DIAMOND) {
            return new DustParticleOptions(new Vector3f(0.25f, 0.85f, 1.0f), scale);
        } else if (item == Items.ENDER_PEARL) {
            return new DustParticleOptions(new Vector3f(0.50f, 0.12f, 0.82f), scale);
        } else if (item == Items.HEART_OF_THE_SEA) {
            return new DustParticleOptions(new Vector3f(0.12f, 0.75f, 0.98f), scale);
        } else if (item == Items.AMETHYST_SHARD) {
            return new DustParticleOptions(new Vector3f(0.85f, 0.45f, 1.0f), scale);
        } else if (item == Items.NETHER_STAR) {
            return new DustParticleOptions(new Vector3f(1.0f, 0.92f, 0.40f), scale);
        } else if (item == Items.EXPERIENCE_BOTTLE) {
            return new DustParticleOptions(new Vector3f(0.65f, 1.0f, 0.25f), scale);
        }
        return new DustParticleOptions(new Vector3f(0.80f, 0.60f, 1.0f), scale);
    }

    public static void spawnRitualParticles(Level level, BlockPos pos, BetterDisenchanterBlockEntity entity) {
        if (!BetterDisenchanterConfig.isMagicCircleEnabled()) return;

        RandomSource random = level.random;
        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        double baseY = pos.getY() + 0.04D; // 紧贴方块底座地面，形成地面魔法阵

        double weaponX = pos.getX() + 0.5D;
        double weaponY = pos.getY() + BetterDisenchanterConfig.getFloatingItemYOffset();
        double weaponZ = pos.getZ() + 0.5D;
        double bookX = pos.getX() + 0.5D;
        double bookY = pos.getY() + 0.82D;
        double bookZ = pos.getZ() + 0.5D;

        ItemStack catalyst = entity.getActiveCatalyst();
        Item catItem = catalyst.getItem();
        DustParticleOptions themedDust = getThemedDust(catalyst, 1.25f);

        // 1. 装备附魔抽离流 (Extracting Energy Stream): 武器 -> 书本
        for (int i = 0; i < 2; i++) {
            double streamAngle = (entity.ticks * 0.5D) + (i * Math.PI) + (random.nextDouble() * 0.2D);
            double radius = 0.20D + random.nextDouble() * 0.1D;
            double startX = weaponX + Math.cos(streamAngle) * radius;
            double startZ = weaponZ + Math.sin(streamAngle) * radius;
            double startY = weaponY + (random.nextDouble() - 0.5D) * 0.15D;

            level.addParticle(ParticleTypes.ENCHANT, startX, startY, startZ,
                    bookX - startX, bookY - startY, bookZ - startZ);
        }

        // 2. 动态收缩与向心汇聚的扩大地面魔法阵 (Wide Initial Array Contracting Inward)
        // 随着仪式进行 (ticks 0 -> 30)，法阵从扩大外围 (外径 1.65D) 逐步旋转并向祛魔台底座平滑收缩 (最终缩小至 0.70D)
        double progress = Math.min(1.0D, entity.ticks / 30.0D);
        double shrink = 1.0D - Math.pow(progress, 0.75D); // 非线性平滑内敛收缩曲线

        double outerRadius = 0.70D + 0.95D * shrink; // 从 1.65D 逐渐收敛至 0.70D
        double innerRadius = 0.42D + 0.73D * shrink; // 从 1.15D 逐渐收敛至 0.42D

        // 旋转角速度随收缩逐渐加速 (类似角动量守恒，聚能感极强)
        double rotOuter = entity.ticks * (0.10D + progress * 0.22D);
        double rotInner = -(entity.ticks * (0.13D + progress * 0.26D));

        // 外环魔法阵圈 (12 个等分点，顺时针加速向内收敛)
        for (int i = 0; i < 12; i++) {
            double angle = rotOuter + (i * (Math.PI * 2.0D / 12.0D));
            double px = centerX + Math.cos(angle) * outerRadius;
            double pz = centerZ + Math.sin(angle) * outerRadius;
            level.addParticle(themedDust, px, baseY, pz, 0.0D, 0.003D, 0.0D);
        }

        // 内环魔法阵圈 (8 个等分点，逆时针交错加速向内收敛)
        for (int i = 0; i < 8; i++) {
            double angle = rotInner + (i * (Math.PI * 2.0D / 8.0D));
            double px = centerX + Math.cos(angle) * innerRadius;
            double pz = centerZ + Math.sin(angle) * innerRadius;
            level.addParticle(themedDust, px, baseY, pz, 0.0D, 0.003D, 0.0D);
        }

        // 向心聚能流粒子：从外环边缘不断向中心汇聚流动的微光
        for (int i = 0; i < 3; i++) {
            double inAngle = random.nextDouble() * Math.PI * 2.0D;
            double inDist = outerRadius + 0.08D + random.nextDouble() * 0.15D;
            double inX = centerX + Math.cos(inAngle) * inDist;
            double inZ = centerZ + Math.sin(inAngle) * inDist;
            double inSpeed = 0.06D + progress * 0.08D;
            level.addParticle(themedDust, inX, baseY, inZ,
                    -Math.cos(inAngle) * inSpeed, 0.005D, -Math.sin(inAngle) * inSpeed);
        }

        // 4 个正交地表法阵节点：随外环同步向内收缩，喷薄微型垂直符文光柱与催化剂专属特效
        for (int i = 0; i < 4; i++) {
            double nodeAngle = rotOuter + (i * (Math.PI / 2.0D));
            double nx = centerX + Math.cos(nodeAngle) * outerRadius;
            double nz = centerZ + Math.sin(nodeAngle) * outerRadius;

            // 地表升腾符文
            level.addParticle(ParticleTypes.ENCHANT, nx, baseY, nz, 0.0D, 0.15D, 0.0D);

            // 专属地表法阵节点效果
            if (catItem == Items.EMERALD) {
                if (random.nextFloat() < 0.35f) {
                    level.addParticle(ParticleTypes.HAPPY_VILLAGER, nx, baseY + 0.02D, nz, 0, 0.02D, 0);
                }
            } else if (catItem == Items.DIAMOND) {
                if (random.nextFloat() < 0.35f) {
                    level.addParticle(ParticleTypes.INSTANT_EFFECT, nx, baseY + 0.02D, nz, 0, 0.02D, 0);
                }
            } else if (catItem == Items.ENDER_PEARL) {
                if (random.nextFloat() < 0.40f) {
                    level.addParticle(ParticleTypes.REVERSE_PORTAL, nx, baseY, nz, 0, 0.04D, 0);
                }
            } else if (catItem == Items.HEART_OF_THE_SEA) {
                if (random.nextFloat() < 0.40f) {
                    level.addParticle(ParticleTypes.NAUTILUS, nx, baseY + 0.02D, nz, 0, 0.03D, 0);
                }
            } else if (catItem == Items.AMETHYST_SHARD) {
                if (random.nextFloat() < 0.40f) {
                    level.addParticle(ParticleTypes.WAX_ON, nx, baseY + 0.02D, nz, 0, 0.01D, 0);
                }
            } else if (catItem == Items.NETHER_STAR) {
                level.addParticle(ParticleTypes.END_ROD, nx, baseY, nz, 0, 0.04D, 0);
            } else if (catItem == Items.EXPERIENCE_BOTTLE) {
                if (random.nextFloat() < 0.40f) {
                    level.addParticle(ParticleTypes.WITCH, nx, baseY + 0.02D, nz, 0, 0.02D, 0);
                }
            }
        }
    }

    public static void spawnCompletionRing(Level level, BlockPos pos, ItemStack catalyst) {
        if (!BetterDisenchanterConfig.isCompletionBurstEnabled()) return;

        RandomSource random = level.random;
        DustParticleOptions themedDust = getThemedDust(catalyst, 1.5f);
        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        double baseY = pos.getY() + 0.05D; // 地面冲击波高度
        double bookY = pos.getY() + 0.85D;

        // 1. 360 度沿地表剧烈向四周辐射扩散的超密集环形冲击波 (法阵向内收缩到极致后的强力破空炸开)
        int ringCount = 48;
        for (int i = 0; i < ringCount; i++) {
            double angle = (2.0D * Math.PI / ringCount) * i;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double startX = centerX + cos * 0.65D;
            double startZ = centerZ + sin * 0.65D;
            double speed = 0.35D;

            // 主色调冲击波粉尘
            level.addParticle(themedDust, startX, baseY, startZ, cos * speed, 0.015D, sin * speed);

            // 伴随高光冲刺末地烛射线 (形成流光四射的粒子破空感)
            if (i % 2 == 0) {
                level.addParticle(ParticleTypes.END_ROD, startX, baseY, startZ, cos * (speed * 0.95D), 0.02D, sin * (speed * 0.95D));
            }

            // 环形爆裂火花
            if (i % 3 == 0) {
                level.addParticle(ParticleTypes.FIREWORK, startX, baseY + 0.02D, startZ, cos * (speed * 0.75D), 0.03D, sin * (speed * 0.75D));
            }
        }

        // 2. 第二道内圈高速回弹冲击波 (双重层次感)
        int innerBlastCount = 24;
        for (int i = 0; i < innerBlastCount; i++) {
            double angle = (2.0D * Math.PI / innerBlastCount) * i + 0.1D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double startX = centerX + cos * 0.35D;
            double startZ = centerZ + sin * 0.35D;
            double speed = 0.20D;

            level.addParticle(themedDust, startX, baseY, startZ, cos * speed, 0.04D, sin * speed);
        }

        // 3. 中心法阵向上喷薄爆发的璀璨能量烟花喷泉 (Burst Fountain from Center)
        for (int i = 0; i < 28; i++) {
            double px = centerX + (random.nextDouble() - 0.5D) * 0.35D;
            double py = bookY + (random.nextDouble() - 0.5D) * 0.2D;
            double pz = centerZ + (random.nextDouble() - 0.5D) * 0.35D;
            double vx = (random.nextDouble() - 0.5D) * 0.22D;
            double vy = 0.15D + random.nextDouble() * 0.22D;
            double vz = (random.nextDouble() - 0.5D) * 0.22D;

            level.addParticle(ParticleTypes.WAX_OFF, px, py, pz, vx, vy, vz);
            if (i % 2 == 0) {
                level.addParticle(ParticleTypes.ENCHANT, px, py, pz, vx * 1.5D, vy * 1.2D, vz * 1.5D);
            }
        }

        // 4. 催化剂专属终结大招特效
        if (catalyst.is(Items.NETHER_STAR)) {
            level.addParticle(ParticleTypes.FLASH, centerX, baseY + 0.5D, centerZ, 0, 0, 0);
            for (int i = 0; i < 16; i++) {
                double vx = (random.nextDouble() - 0.5D) * 0.3D;
                double vy = 0.1D + random.nextDouble() * 0.3D;
                double vz = (random.nextDouble() - 0.5D) * 0.3D;
                level.addParticle(ParticleTypes.END_ROD, centerX, bookY, centerZ, vx, vy, vz);
            }
        } else if (catalyst.is(Items.EMERALD)) {
            for (int i = 0; i < 12; i++) {
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, centerX + (random.nextDouble() - 0.5D) * 0.6D,
                        bookY + random.nextDouble() * 0.4D, centerZ + (random.nextDouble() - 0.5D) * 0.6D,
                        0, 0.05D, 0);
            }
        } else if (catalyst.is(Items.AMETHYST_SHARD)) {
            for (int i = 0; i < 16; i++) {
                level.addParticle(ParticleTypes.WAX_ON, centerX + (random.nextDouble() - 0.5D) * 0.5D,
                        bookY + random.nextDouble() * 0.3D, centerZ + (random.nextDouble() - 0.5D) * 0.5D,
                        (random.nextDouble() - 0.5D) * 0.1D, 0.08D, (random.nextDouble() - 0.5D) * 0.1D);
            }
        }
    }

    public static void playCompletionSounds(Level level, BlockPos pos, ItemStack catalyst, boolean keepItem) {
        // 1. MC原版羽毛笔书写附魔与翻书声 (Vanilla writing sound on parchment & page flip)
        level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0f, 1.0f);

        // 2. 厚重古籍皮革闭合声 (Heavy Ancient Tome Snap)
        level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.1f, 0.95f);
        level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.45f, 1.8f);

        // 3. 附魔封印结晶与附魔台清脆回响
        level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.9f, 1.25f);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.3f);

        // 4. 仪式成功清脆奖励音 (Levelup chime)
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.6f);

        // 5. 原装备损毁破碎声反馈 (若未保留原装备)
        if (!keepItem) {
            level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.65f, 1.15f);
        }
    }

    public ItemInteractionResult onUseItem(ItemStack handStack, Player player, InteractionHand hand) {
        InteractionResult result = handlePlayerInteraction(player, handStack, hand);
        if (result.consumesAction()) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public InteractionResult onUseEmptyHand(Player player) {
        return handlePlayerInteraction(player, player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND);
    }

    public InteractionResult handlePlayerInteraction(Player player, ItemStack handStack, InteractionHand hand) {
        if (level == null) return InteractionResult.PASS;

        // 1. 仪式进行中，绝对保护
        if (status == Status.ENCHANTING) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 2. 祛魔结束后，可直接右键拿取附魔书 (不管是空手、手持物品、站立还是蹲下)
        if (book.is(Items.ENCHANTED_BOOK)) {
            if (!level.isClientSide) {
                giveOrDrop(player, book);
                book = ItemStack.EMPTY;
                status = Status.PASSIVE;
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
                notifyListeners();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 3. 取消祛魔时应该蹲下右键拿下物品 (Sneak + Right Click to Cancel / Retrieve)
        if (player.isShiftKeyDown()) {
            if (!item.isEmpty()) {
                if (!level.isClientSide) {
                    giveOrDrop(player, item);
                    item = ItemStack.EMPTY;
                    status = Status.PASSIVE;
                    level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
                    notifyListeners();
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else if (!book.isEmpty()) {
                if (!level.isClientSide) {
                    giveOrDrop(player, book);
                    book = ItemStack.EMPTY;
                    status = Status.PASSIVE;
                    level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
                    notifyListeners();
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }

        // 4. 站立状态下的物品放置与仪式触发 (Standing Right-Click)

        // 4A. 手持普通书本 (Regular Book)
        if (handStack.is(Items.BOOK)) {
            if (book.isEmpty()) {
                if (!level.isClientSide) {
                    book = handStack.copyWithCount(1);
                    if (!player.isCreative()) handStack.shrink(1);
                    bookOpenAngle = 0;
                    status = Status.PASSIVE;
                    level.playSound(null, worldPosition, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 0.8f, 0.8f);
                    notifyListeners();
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        // 4B. 手持附魔装备 (Enchanted Weapon/Armor/Tool/Book)
        if (hasEnchantments(handStack)) {
            // 严格检验：必须先放置普通书本！
            if (book.isEmpty()) {
                if (level.isClientSide) {
                    player.displayClientMessage(Component.translatable("text.betterdisenchanter.need_book"), true);
                }
                level.playSound(null, worldPosition, SoundEvents.CRAFTER_FAIL, SoundSource.BLOCKS, 0.6f, 1.8f);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (!book.is(Items.BOOK)) {
                return InteractionResult.PASS;
            }

            // Case 4B-1: 台面上保留有上一次仪式的未附魔原装备 -> 一键丝滑换装
            if (!item.isEmpty() && !hasEnchantments(item)) {
                if (!level.isClientSide) {
                    giveOrDrop(player, item);
                    item = handStack.copyWithCount(1);
                    if (!player.isCreative()) handStack.shrink(1);
                    status = Status.WAITING;
                    level.playSound(null, worldPosition, SoundEvents.CHISELED_BOOKSHELF_INSERT_ENCHANTED, SoundSource.BLOCKS, 0.8f, 1.2f);
                    notifyListeners();
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // Case 4B-2: 装备槽位为空 -> 放置附魔装备
            if (item.isEmpty()) {
                if (!level.isClientSide) {
                    item = handStack.copyWithCount(1);
                    if (!player.isCreative()) handStack.shrink(1);
                    status = Status.WAITING;
                    level.playSound(null, worldPosition, SoundEvents.CHISELED_BOOKSHELF_INSERT_ENCHANTED, SoundSource.BLOCKS, 0.8f, 1.2f);
                    notifyListeners();
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            return InteractionResult.PASS;
        }

        // 4C. 手持催化剂，且台面就绪 (Status.WAITING: 书与附魔装备均就绪)
        boolean isReady = (status == Status.WAITING) || (!book.isEmpty() && !item.isEmpty() && hasEnchantments(item));
        if (isReady) {
            if (!handStack.isEmpty()) {
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
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    } else {
                        if (level.isClientSide) {
                            player.displayClientMessage(Component.translatable("text.betterdisenchanter.extra_catalysts_required", requiredCount - handStack.getCount()), true);
                        }
                        level.playSound(null, worldPosition, SoundEvents.CRAFTER_FAIL, SoundSource.BLOCKS, 0.8f, 2.0f);
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    }
                }
            }

            // 4D. 空手右键交互，完成基础祛魔任务 (Empty Hand Right-Click to start/complete disenchanting!)
            if (handStack.isEmpty()) {
                // 如果是副手触发且主手持有物品，不拦截副手
                if (hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) {
                    return InteractionResult.PASS;
                }

                if (BetterDisenchanterConfig.isNoCatalystAllowed()) {
                    if (!level.isClientSide) {
                        activeCatalyst = ItemStack.EMPTY;
                        status = Status.ENCHANTING;
                        ticks = 0;
                        playCatalystStartSound(level, worldPosition, ItemStack.EMPTY);
                        notifyListeners();
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                } else {
                    if (level.isClientSide) {
                        player.displayClientMessage(Component.translatable("text.betterdisenchanter.need_catalyst"), true);
                    }
                    level.playSound(null, worldPosition, SoundEvents.CRAFTER_FAIL, SoundSource.BLOCKS, 0.6f, 1.8f);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }

        // 4E. 若台面上还残留有上一次仪式保留的未附魔装备，且空手点击 -> 取下该装备
        if (handStack.isEmpty() && !item.isEmpty() && !hasEnchantments(item)) {
            if (hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                giveOrDrop(player, item);
                item = ItemStack.EMPTY;
                status = Status.PASSIVE;
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
                notifyListeners();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 4F. 如果台面只放置了书本，而玩家空手右键 -> 友好提示需要放置附魔装备
        if (handStack.isEmpty() && !book.isEmpty() && item.isEmpty()) {
            if (hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable("text.betterdisenchanter.need_item"), true);
            }
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, 1.8f);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    public void onPunch(Player player) {
        if (level == null || level.isClientSide) return;
        if (status == Status.ENCHANTING) return;

        // 1. 祛魔结束后，可直接左键拿取附魔书！
        if (book.is(Items.ENCHANTED_BOOK)) {
            giveOrDrop(player, book);
            book = ItemStack.EMPTY;
            status = Status.PASSIVE;
            level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
            notifyListeners();
            return;
        }

        // 2. 正常状态下左键敲击：优先拿取装备，再拿取书本
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

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, lookupProvider);
        }
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
        if (status == Status.ENCHANTING) return false;
        if (stack.getCount() == 1) {
            if (slot == 0) {
                return book.isEmpty() && stack.is(Items.BOOK);
            } else if (slot == 1) {
                return !book.isEmpty() && book.is(Items.BOOK) && item.isEmpty() && hasEnchantments(stack);
            }
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (status == Status.ENCHANTING) return false;
        if (slot == 0) {
            return book.is(Items.ENCHANTED_BOOK);
        } else if (slot == 1) {
            return !item.isEmpty() && !hasEnchantments(item);
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
        if (item.isEmpty() || book.isEmpty()) {
            if (status != Status.ENCHANTING) {
                status = Status.PASSIVE;
            }
        }
        notifyListeners();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        if (slot == 0) book = ItemStack.EMPTY;
        else item = ItemStack.EMPTY;
        if (item.isEmpty() || book.isEmpty()) {
            if (status != Status.ENCHANTING) {
                status = Status.PASSIVE;
            }
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            book = stack;
        } else if (slot == 1) {
            item = stack;
        }
        if (!book.isEmpty() && !item.isEmpty() && hasEnchantments(item) && book.is(Items.BOOK)) {
            status = Status.WAITING;
        } else if (status != Status.ENCHANTING) {
            status = Status.PASSIVE;
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
