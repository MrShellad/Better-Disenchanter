package com.betterdisenchanter;

import com.betterdisenchanter.block.BetterDisenchanterBlock;
import com.betterdisenchanter.block.entity.BetterDisenchanterBlockEntity;
import com.betterdisenchanter.client.BetterDisenchanterClient;
import com.betterdisenchanter.recipe.ModRecipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BetterDisenchanter.MOD_ID)
public class BetterDisenchanter {
    public static final String MOD_ID = "betterdisenchanter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredBlock<BetterDisenchanterBlock> DISENCHANTER_BLOCK = BLOCKS.register("disenchanter",
            () -> new BetterDisenchanterBlock(BlockBehaviour.Properties.of()
                    .strength(4.0f, 1.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 7)));

    public static final DeferredItem<BlockItem> DISENCHANTER_ITEM = ITEMS.register("disenchanter",
            () -> new BlockItem(DISENCHANTER_BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BetterDisenchanterBlockEntity>> DISENCHANTER_BE =
            BLOCK_ENTITY_TYPES.register("disenchanter",
                    () -> new BlockEntityType<>(BetterDisenchanterBlockEntity::new, DISENCHANTER_BLOCK.get()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("betterdisenchanter_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.betterdisenchanter"))
                    .icon(() -> new ItemStack(DISENCHANTER_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(DISENCHANTER_ITEM.get());
                    })
                    .build());

    public BetterDisenchanter(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        ModRecipes.register(modEventBus);

        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, BetterDisenchanterConfig.CLIENT_SPEC);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, BetterDisenchanterConfig.COMMON_SPEC);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            BetterDisenchanterClient.init(modEventBus, modContainer);
        }

        LOGGER.info("Better Disenchanter initialized!");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
