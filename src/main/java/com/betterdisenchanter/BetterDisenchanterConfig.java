package com.betterdisenchanter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Collections;
import java.util.List;

public class BetterDisenchanterConfig {

    // ==========================================
    // 客户端视觉配置 (Client Spec)
    // ==========================================
    public static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec CLIENT_SPEC;

    // Visual Scales
    public static final ModConfigSpec.DoubleValue FLOATING_ITEM_SCALE;
    public static final ModConfigSpec.DoubleValue CATALYST_ITEM_SCALE;
    public static final ModConfigSpec.DoubleValue TEXT_DISPLAY_SCALE;

    // Visual Heights
    public static final ModConfigSpec.DoubleValue FLOATING_ITEM_Y_OFFSET;
    public static final ModConfigSpec.DoubleValue CATALYST_ITEM_Y_OFFSET;
    public static final ModConfigSpec.DoubleValue DISPLAY_BILLBOARD_Y_OFFSET;

    // Visual Display Options
    public static final ModConfigSpec.BooleanValue SHOW_TEXT_BACKGROUND;
    public static final ModConfigSpec.BooleanValue TEXT_DROP_SHADOW;

    // Particle Options
    public static final ModConfigSpec.BooleanValue ENABLE_PARTICLES;
    public static final ModConfigSpec.BooleanValue ENABLE_RITUAL_MAGIC_CIRCLE;
    public static final ModConfigSpec.BooleanValue ENABLE_COMPLETION_BURST;

    // ==========================================
    // 通用玩法与催化剂配置 (Common Spec)
    // ==========================================
    public static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec COMMON_SPEC;

    public static final ModConfigSpec.BooleanValue ALLOW_DISENCHANTING_WITHOUT_CATALYST;

    public static final CatalystConfig CATALYST_EMERALD;
    public static final CatalystConfig CATALYST_DIAMOND;
    public static final CatalystConfig CATALYST_ENDER_PEARL;
    public static final CatalystConfig CATALYST_HEART_OF_THE_SEA;
    public static final CatalystConfig CATALYST_AMETHYST_SHARD;
    public static final CatalystConfig CATALYST_NETHER_STAR;
    public static final CatalystConfig CATALYST_EXPERIENCE_BOTTLE;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_CATALYSTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CUSTOM_CATALYST_COUNTS;

    public static class CatalystConfig {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.IntValue requiredCount;

        public CatalystConfig(ModConfigSpec.Builder builder, String name, String cnName, String cnEffect, int defaultCount) {
            builder.push(name);
            enabled = builder
                    .comment("是否启用 [" + cnName + "] 作为催化剂 (" + cnEffect + ")")
                    .define("enabled", true);
            requiredCount = builder
                    .comment("每次祛魔所需消耗的 [" + cnName + "] 数量 (默认: " + defaultCount + ", 范围: 1 ~ 64)")
                    .defineInRange("requiredCount", defaultCount, 1, 64);
            builder.pop();
        }
    }

    static {
        // -----------------------------------------------------
        // 1. 构建客户端配置 (betterdisenchanter-client.toml)
        // -----------------------------------------------------
        CLIENT_BUILDER.push("visuals");

        CLIENT_BUILDER.comment("缩放大小设置 (Scale settings)");
        FLOATING_ITEM_SCALE = CLIENT_BUILDER
                .comment("翻开的书本上方悬浮附魔武器/物品的缩放大小 (默认: 0.95, 范围: 0.1 ~ 3.0)")
                .defineInRange("floatingItemScale", 0.95D, 0.1D, 3.0D);

        CATALYST_ITEM_SCALE = CLIENT_BUILDER
                .comment("3D 悬浮催化剂物品的缩放大小 (默认: 0.60, 范围: 0.1 ~ 3.0)")
                .defineInRange("catalystItemScale", 0.60D, 0.1D, 3.0D);

        TEXT_DISPLAY_SCALE = CLIENT_BUILDER
                .comment("悬浮文字看板的缩放大小 (默认: 0.022, 范围: 0.005 ~ 0.1)")
                .defineInRange("textDisplayScale", 0.022D, 0.005D, 0.1D);

        CLIENT_BUILDER.comment("悬浮高度设置 (Y offset settings，以祛魔台方块底部为基准，台面高度为 0.75)");
        FLOATING_ITEM_Y_OFFSET = CLIENT_BUILDER
                .comment("悬浮附魔武器距离祛魔台底部的高度 (默认: 1.25, 范围: 0.5 ~ 3.0)")
                .defineInRange("floatingItemYOffset", 1.25D, 0.5D, 3.0D);

        CATALYST_ITEM_Y_OFFSET = CLIENT_BUILDER
                .comment("3D 悬浮催化剂距离祛魔台底部的高度 (默认: 1.75, 范围: 0.5 ~ 4.0)")
                .defineInRange("catalystItemYOffset", 1.75D, 0.5D, 4.0D);

        DISPLAY_BILLBOARD_Y_OFFSET = CLIENT_BUILDER
                .comment("悬浮文字看板距离祛魔台底部的高度 (默认: 2.55, 范围: 0.5 ~ 5.0，默认留出约一行空隙，避免与催化剂重叠)")
                .defineInRange("displayBillboardYOffset", 2.55D, 0.5D, 5.0D);

        CLIENT_BUILDER.comment("文字背景底板与阴影设置 (Display background & shadow)");
        SHOW_TEXT_BACKGROUND = CLIENT_BUILDER
                .comment("是否渲染悬浮文字背后的半透明深色底板 (默认: true, 改为 false 可完全隐藏底板)")
                .define("showTextBackground", true);

        TEXT_DROP_SHADOW = CLIENT_BUILDER
                .comment("是否渲染悬浮文字的阴影 (默认: false, 当关闭背景底板时可开启以增强辨识度)")
                .define("textDropShadow", false);

        CLIENT_BUILDER.comment("粒子特效开关设置 (Particle effect toggles)");
        ENABLE_PARTICLES = CLIENT_BUILDER
                .comment("是否启用祛魔仪式与收尾时的粒子特效总开关 (默认: true，关闭后将不产生任何仪式与收尾粒子)")
                .define("enableParticles", true);

        ENABLE_RITUAL_MAGIC_CIRCLE = CLIENT_BUILDER
                .comment("是否启用仪式过程中的地表旋转收缩魔法阵与装备附魔抽离流粒子 (默认: true)")
                .define("enableRitualMagicCircle", true);

        ENABLE_COMPLETION_BURST = CLIENT_BUILDER
                .comment("是否启用仪式完成瞬间的双层冲击波与烟花喷泉大爆炸粒子 (默认: true)")
                .define("enableCompletionBurst", true);

        CLIENT_BUILDER.pop();
        CLIENT_SPEC = CLIENT_BUILDER.build();

        // -----------------------------------------------------
        // 2. 构建通用玩法配置 (betterdisenchanter-common.toml)
        // -----------------------------------------------------
        COMMON_BUILDER.comment("Better Disenchanter 通用配置 (Common Configuration - 服务端与单人存档通用)");

        COMMON_BUILDER.push("general");
        ALLOW_DISENCHANTING_WITHOUT_CATALYST = COMMON_BUILDER
                .comment("是否允许在不使用任何催化剂的情况下进行基础祛魔 (默认: true，空手右键直接触发，回收第1条附魔并摧毁原装备)")
                .define("allowDisenchantingWithoutCatalyst", true);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("catalysts");
        COMMON_BUILDER.comment("每种催化剂的具体配置：可单独控制是否启用，以及每次祛魔消耗的物品数量 (1~64)");

        CATALYST_EMERALD = new CatalystConfig(COMMON_BUILDER, "emerald", "绿宝石", "回收随机 2 条附魔，摧毁原装备", 1);
        CATALYST_DIAMOND = new CatalystConfig(COMMON_BUILDER, "diamond", "钻石", "回收第 1 条附魔 + 随机 2 条附魔，摧毁原装备", 1);
        CATALYST_ENDER_PEARL = new CatalystConfig(COMMON_BUILDER, "ender_pearl", "末影珍珠", "回收随机 1 条附魔，保留原装备但扣除 500 点耐久", 1);
        CATALYST_HEART_OF_THE_SEA = new CatalystConfig(COMMON_BUILDER, "heart_of_the_sea", "海洋之心", "回收所有附魔但附魔等级降低 1 级，摧毁原装备", 1);
        CATALYST_AMETHYST_SHARD = new CatalystConfig(COMMON_BUILDER, "amethyst_shard", "紫水晶碎片", "回收第 1 条附魔，无损保留原装备", 1);
        CATALYST_NETHER_STAR = new CatalystConfig(COMMON_BUILDER, "nether_star", "下界之星", "回收所有附魔，无损保留原装备", 1);
        CATALYST_EXPERIENCE_BOTTLE = new CatalystConfig(COMMON_BUILDER, "experience_bottle", "附魔之瓶", "回收所有最高等级附魔，摧毁原装备", 1);

        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("custom_catalysts");
        COMMON_BUILDER.comment("扩展与自定义催化剂覆盖设置");

        DISABLED_CATALYSTS = COMMON_BUILDER
                .comment("全局禁用的催化剂物品ID列表 (例如: [\"minecraft:emerald\", \"modid:custom_item\"])")
                .defineListAllowEmpty("disabledCatalysts", Collections.emptyList(), () -> "", o -> o instanceof String);

        CUSTOM_CATALYST_COUNTS = COMMON_BUILDER
                .comment("自定义催化剂消耗数量映射列表，格式为 \"物品ID=数量\" (例如: [\"minecraft:emerald=5\", \"minecraft:diamond=2\"])")
                .defineListAllowEmpty("customRequiredCounts", Collections.emptyList(), () -> "", o -> o instanceof String);

        COMMON_BUILDER.pop();
        COMMON_SPEC = COMMON_BUILDER.build();
    }

    // ==========================================
    // 客户端 Getter 方法
    // ==========================================

    public static float getFloatingItemScale() {
        try {
            return FLOATING_ITEM_SCALE.get().floatValue();
        } catch (Exception e) {
            return 0.95f;
        }
    }

    public static float getCatalystItemScale() {
        try {
            return CATALYST_ITEM_SCALE.get().floatValue();
        } catch (Exception e) {
            return 0.60f;
        }
    }

    public static float getTextDisplayScale() {
        try {
            return TEXT_DISPLAY_SCALE.get().floatValue();
        } catch (Exception e) {
            return 0.022f;
        }
    }

    public static double getFloatingItemYOffset() {
        try {
            return FLOATING_ITEM_Y_OFFSET.get();
        } catch (Exception e) {
            return 1.25D;
        }
    }

    public static double getCatalystItemYOffset() {
        try {
            return CATALYST_ITEM_Y_OFFSET.get();
        } catch (Exception e) {
            return 1.75D;
        }
    }

    public static double getDisplayBillboardYOffset() {
        try {
            return DISPLAY_BILLBOARD_Y_OFFSET.get();
        } catch (Exception e) {
            return 2.55D;
        }
    }

    public static boolean isTextBackgroundEnabled() {
        try {
            return SHOW_TEXT_BACKGROUND.get();
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean isTextDropShadowEnabled() {
        try {
            return TEXT_DROP_SHADOW.get();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isParticlesEnabled() {
        try {
            return ENABLE_PARTICLES.get();
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean isMagicCircleEnabled() {
        try {
            return isParticlesEnabled() && ENABLE_RITUAL_MAGIC_CIRCLE.get();
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean isCompletionBurstEnabled() {
        try {
            return isParticlesEnabled() && ENABLE_COMPLETION_BURST.get();
        } catch (Exception e) {
            return true;
        }
    }

    // ==========================================
    // 通用/服务端 催化剂查询方法
    // ==========================================

    public static boolean isNoCatalystAllowed() {
        try {
            return ALLOW_DISENCHANTING_WITHOUT_CATALYST.get();
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean isCatalystEnabled(Item item) {
        try {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String idStr = id.toString();

            // 1. 检查全局禁用列表
            List<? extends String> disabled = DISABLED_CATALYSTS.get();
            if (disabled != null && disabled.contains(idStr)) {
                return false;
            }

            // 2. 检查内置 7 种催化剂的各自独立开关
            if (item == Items.EMERALD) return CATALYST_EMERALD.enabled.get();
            if (item == Items.DIAMOND) return CATALYST_DIAMOND.enabled.get();
            if (item == Items.ENDER_PEARL) return CATALYST_ENDER_PEARL.enabled.get();
            if (item == Items.HEART_OF_THE_SEA) return CATALYST_HEART_OF_THE_SEA.enabled.get();
            if (item == Items.AMETHYST_SHARD) return CATALYST_AMETHYST_SHARD.enabled.get();
            if (item == Items.NETHER_STAR) return CATALYST_NETHER_STAR.enabled.get();
            if (item == Items.EXPERIENCE_BOTTLE) return CATALYST_EXPERIENCE_BOTTLE.enabled.get();

            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public static int getCatalystRequiredCount(Item item, int defaultCount) {
        try {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String idStr = id.toString();

            // 1. 优先检查自定义覆盖列表 (格式 "物品ID=数量")
            List<? extends String> customList = CUSTOM_CATALYST_COUNTS.get();
            if (customList != null) {
                for (String entry : customList) {
                    String[] parts = entry.split("=");
                    if (parts.length == 2 && parts[0].trim().equalsIgnoreCase(idStr)) {
                        try {
                            return Math.max(1, Math.min(64, Integer.parseInt(parts[1].trim())));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // 2. 检查内置 7 种催化剂的消耗数量配置
            if (item == Items.EMERALD) return CATALYST_EMERALD.requiredCount.get();
            if (item == Items.DIAMOND) return CATALYST_DIAMOND.requiredCount.get();
            if (item == Items.ENDER_PEARL) return CATALYST_ENDER_PEARL.requiredCount.get();
            if (item == Items.HEART_OF_THE_SEA) return CATALYST_HEART_OF_THE_SEA.requiredCount.get();
            if (item == Items.AMETHYST_SHARD) return CATALYST_AMETHYST_SHARD.requiredCount.get();
            if (item == Items.NETHER_STAR) return CATALYST_NETHER_STAR.requiredCount.get();
            if (item == Items.EXPERIENCE_BOTTLE) return CATALYST_EXPERIENCE_BOTTLE.requiredCount.get();

            return defaultCount;
        } catch (Exception e) {
            return defaultCount;
        }
    }
}
