package com.betterdisenchanter.client;

import com.betterdisenchanter.BetterDisenchanterConfig;
import com.betterdisenchanter.block.entity.BetterDisenchanterBlockEntity;
import com.betterdisenchanter.recipe.CatalystRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BetterDisenchanterBlockEntityRenderer
        implements BlockEntityRenderer<BetterDisenchanterBlockEntity, BetterDisenchanterBlockEntityRenderer.RenderState> {

    private final BookModel bookModel;
    private final SpriteGetter sprites;
    private final ItemModelResolver itemModelResolver;
    private final Font font;

    public BetterDisenchanterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
        this.sprites = context.sprites();
        this.itemModelResolver = context.itemModelResolver();
        this.font = context.font();
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(
            BetterDisenchanterBlockEntity entity,
            RenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        if (entity.getLevel() == null) return;

        float gameTime = entity.getLevel().getGameTime() + partialTicks;
        int seed = (int) entity.getBlockPos().asLong();

        // 1. Book State
        ItemStack bookStack = entity.getBook();
        state.hasBook = !bookStack.isEmpty();
        if (state.hasBook) {
            state.bookHasFoil = bookStack.is(Items.ENCHANTED_BOOK) || bookStack.hasFoil();
            state.bookTime = gameTime;
            state.bookRot = -(entity.bookLastRot + BetterDisenchanterBlockEntity.aroundRadial(entity.bookRot - entity.bookLastRot) * partialTicks);
            boolean isEnchanting = entity.getStatus() == BetterDisenchanterBlockEntity.Status.ENCHANTING;
            state.bookOpenAngle = entity.bookLastOpenAngle + (entity.bookOpenAngle - entity.bookLastOpenAngle) * partialTicks;
            state.bookPageFlip = isEnchanting ? (Mth.sin(gameTime * 0.45F) * 0.18F + 0.18F) : 0.0F;
        }

        // 2. Floating Enchanted Item
        ItemStack itemStack = entity.getItem();
        state.hasFloatingItem = (entity.bookOpenAngle >= 0.8f && !itemStack.isEmpty());
        if (state.hasFloatingItem) {
            state.floatingItemGameTime = gameTime;
            state.floatingItemIsEnchanting = (entity.getStatus() == BetterDisenchanterBlockEntity.Status.ENCHANTING);
            state.floatingItemShakeProgress = Math.min(1.0F, entity.ticks / 30.0F);
            this.itemModelResolver.updateForTopItem(
                    state.floatingItemRenderState,
                    itemStack,
                    ItemDisplayContext.GROUND,
                    entity.getLevel(),
                    null,
                    seed
            );
        } else {
            state.floatingItemRenderState.clear();
        }

        // 3. Catalyst Item & HUD Display Billboard
        boolean isWaiting = entity.getStatus() == BetterDisenchanterBlockEntity.Status.WAITING;
        boolean isEnchanting = entity.getStatus() == BetterDisenchanterBlockEntity.Status.ENCHANTING;

        state.showHud = false;
        state.hasCatalystItem = false;
        state.catalystItemRenderState.clear();

        if (isWaiting || isEnchanting) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player != null) {
                ItemStack displayStack = ItemStack.EMPTY;
                CatalystRecipe recipe = null;
                boolean hasCount = true;

                if (isEnchanting) {
                    displayStack = entity.getActiveCatalyst();
                    if (displayStack.isEmpty()) {
                        recipe = CatalystRecipe.DEFAULT_PROCESSOR;
                    } else {
                        recipe = entity.findMatchingCatalystRecipe(displayStack);
                    }
                } else if (player.distanceToSqr(Vec3.atCenterOf(entity.getBlockPos())) < 36.0D) {
                    ItemStack held = player.getMainHandItem();
                    if (held.isEmpty()) {
                        held = player.getOffhandItem();
                    }
                    if (!held.isEmpty()) {
                        recipe = entity.findMatchingCatalystRecipe(held);
                        if (recipe != null) {
                            int requiredCount = recipe.getRequiredCount(held);
                            displayStack = held.copyWithCount(requiredCount);
                            hasCount = held.getCount() >= requiredCount;
                        }
                    } else if (BetterDisenchanterConfig.isNoCatalystAllowed()) {
                        recipe = CatalystRecipe.DEFAULT_PROCESSOR;
                        displayStack = ItemStack.EMPTY;
                    }
                }

                if (recipe != null) {
                    state.showHud = true;
                    state.billboardGameTime = gameTime;
                    state.hasCatalystForY = !displayStack.isEmpty();

                    if (!displayStack.isEmpty()) {
                        state.hasCatalystItem = true;
                        state.catalystGameTime = gameTime;
                        state.catalystIsEnchanting = isEnchanting;
                        this.itemModelResolver.updateForTopItem(
                                state.catalystItemRenderState,
                                displayStack,
                                ItemDisplayContext.GROUND,
                                entity.getLevel(),
                                null,
                                seed + 1
                        );
                    }

                    // Build Title
                    MutableComponent titleComponent;
                    if (!displayStack.isEmpty()) {
                        int requiredCount = recipe.getRequiredCount(displayStack);
                        titleComponent = Component.empty()
                                .append(displayStack.getHoverName())
                                .append(" x" + requiredCount)
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
                        if (!hasCount) {
                            titleComponent.append(Component.translatable("text.betterdisenchanter.not_enough").withStyle(ChatFormatting.RED));
                        }
                    } else {
                        titleComponent = Component.translatable("betterdisenchanter.catalyst.none.title")
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
                    }

                    // Build Action
                    MutableComponent actionComponent;
                    if (isEnchanting) {
                        actionComponent = Component.translatable("betterdisenchanter.ritual.in_progress")
                                .withStyle(ChatFormatting.LIGHT_PURPLE);
                    } else if (!recipe.descriptionKey.isEmpty()) {
                        actionComponent = Component.translatable(recipe.descriptionKey).withStyle(ChatFormatting.WHITE);
                    } else {
                        actionComponent = Component.literal(recipe.action).withStyle(ChatFormatting.GRAY);
                    }

                    // Build Result Badge
                    MutableComponent resultBadge;
                    if (recipe.keepItem) {
                        if (recipe.damageItem > 0) {
                            resultBadge = Component.translatable("betterdisenchanter.catalyst.keep_with_damage", recipe.damageItem)
                                    .withStyle(ChatFormatting.YELLOW);
                        } else {
                            resultBadge = Component.translatable("betterdisenchanter.catalyst.keep_item")
                                    .withStyle(ChatFormatting.GREEN);
                        }
                    } else {
                        resultBadge = Component.translatable("betterdisenchanter.catalyst.item_consumed")
                                .withStyle(ChatFormatting.RED);
                    }

                    state.line1 = cleanText(Component.empty().append(titleComponent).append(" ➔ ").append(resultBadge));
                    state.line2 = cleanText(actionComponent);
                    state.w1 = this.font.width(state.line1);
                    state.w2 = this.font.width(state.line2);
                }
            }
        }
    }

    @Override
    public void submit(
            RenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        // 1. Render Book
        if (state.hasBook) {
            poseStack.pushPose();
            poseStack.translate(0.5D, 0.75D, 0.5D);
            poseStack.translate(0.0D, 0.1F + Mth.sin(state.bookTime * 0.1F) * 0.01F, 0.0D);

            poseStack.mulPose(Axis.YP.rotation(state.bookRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));

            BookModel.State bookState = BookModel.State.forAnimation(
                    state.bookTime,
                    state.bookPageFlip,
                    state.bookPageFlip * 0.85F,
                    state.bookOpenAngle
            );

            submitNodeCollector.submitModel(
                    this.bookModel,
                    bookState,
                    poseStack,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    -1,
                    EnchantTableRenderer.BOOK_TEXTURE,
                    this.sprites,
                    0,
                    state.breakProgress
            );

            if (state.bookHasFoil) {
                submitNodeCollector.order(1).submitModel(
                        this.bookModel,
                        bookState,
                        poseStack,
                        RenderTypes.entityGlint(),
                        state.lightCoords,
                        OverlayTexture.NO_OVERLAY,
                        -1,
                        this.sprites.get(EnchantTableRenderer.BOOK_TEXTURE),
                        0,
                        null
                );
            }

            poseStack.popPose();
        }

        // 2. Render Floating Enchanted Item
        if (state.hasFloatingItem && !state.floatingItemRenderState.isEmpty()) {
            poseStack.pushPose();
            double itemY = BetterDisenchanterConfig.getFloatingItemYOffset();
            poseStack.translate(0.5D, itemY, 0.5D);

            float bob = Mth.sin(state.floatingItemGameTime * 0.12F) * 0.025F;
            poseStack.translate(0.0D, bob, 0.0D);

            float rotSpeed = 2.2F;
            if (state.floatingItemIsEnchanting) {
                rotSpeed = 2.2F + state.floatingItemShakeProgress * 15.0F;

                float shakeMagnitude = state.floatingItemShakeProgress * 0.015F;
                float shakeX = Mth.sin(state.floatingItemGameTime * 2.2F) * shakeMagnitude;
                float shakeZ = Mth.cos(state.floatingItemGameTime * 2.6F) * shakeMagnitude;
                poseStack.translate(shakeX, 0.0D, shakeZ);
            }

            poseStack.mulPose(Axis.YP.rotationDegrees(state.floatingItemGameTime * rotSpeed));
            float itemScale = BetterDisenchanterConfig.getFloatingItemScale();
            poseStack.scale(itemScale, itemScale, itemScale);

            state.floatingItemRenderState.submit(
                    poseStack,
                    submitNodeCollector,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );

            poseStack.popPose();
        }

        // 3. Render High-Version Native Item Display Effect (Centered Catalyst & Text Display Billboard)
        if (state.showHud) {
            // 3a. Catalyst Item
            if (state.hasCatalystItem && !state.catalystItemRenderState.isEmpty()) {
                poseStack.pushPose();
                double catalystY = BetterDisenchanterConfig.getCatalystItemYOffset();
                poseStack.translate(0.5D, catalystY, 0.5D);

                float itemBob = Mth.sin(state.catalystGameTime * 0.12F) * 0.02F;
                poseStack.translate(0.0D, itemBob, 0.0D);

                float rotSpeed = state.catalystIsEnchanting ? 6.0F : 3.0F;
                poseStack.mulPose(Axis.YP.rotationDegrees(state.catalystGameTime * rotSpeed));
                float catalystScale = BetterDisenchanterConfig.getCatalystItemScale();
                poseStack.scale(catalystScale, catalystScale, catalystScale);

                state.catalystItemRenderState.submit(
                        poseStack,
                        submitNodeCollector,
                        LightCoordsUtil.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        0
                );

                poseStack.popPose();
            }

            // 3b. Text Display Billboard
            poseStack.pushPose();
            double billboardY = BetterDisenchanterConfig.getDisplayBillboardYOffset();
            double textY = state.hasCatalystForY ? billboardY : (billboardY - 0.65D);
            poseStack.translate(0.5D, textY, 0.5D);

            float textBob = Mth.sin(state.billboardGameTime * 0.08F) * 0.015F;
            poseStack.translate(0.0D, textBob, 0.0D);

            // Billboard facing camera
            poseStack.mulPose(camera.orientation);
            float textScale = BetterDisenchanterConfig.getTextDisplayScale();
            poseStack.scale(textScale, -textScale, textScale);

            float x1 = -state.w1 / 2.0F;
            float x2 = -state.w2 / 2.0F;

            if (BetterDisenchanterConfig.isTextBackgroundEnabled()) {
                float halfW = Math.max(state.w1, state.w2) / 2.0F + 4.0F;
                float minX = -halfW;
                float maxX = halfW;
                float minY = -13.0F;
                float maxY = 13.0F;

                submitNodeCollector.submitCustomGeometry(
                        poseStack,
                        RenderTypes.textBackground(),
                        (lambdaPose, buffer) -> {
                            buffer.addVertex(lambdaPose, minX, minY, -0.01F).setColor(0x80000000).setLight(LightCoordsUtil.FULL_BRIGHT);
                            buffer.addVertex(lambdaPose, minX, maxY, -0.01F).setColor(0x80000000).setLight(LightCoordsUtil.FULL_BRIGHT);
                            buffer.addVertex(lambdaPose, maxX, maxY, -0.01F).setColor(0x80000000).setLight(LightCoordsUtil.FULL_BRIGHT);
                            buffer.addVertex(lambdaPose, maxX, minY, -0.01F).setColor(0x80000000).setLight(LightCoordsUtil.FULL_BRIGHT);
                        }
                );
            }

            boolean dropShadow = BetterDisenchanterConfig.isTextDropShadowEnabled();
            OrderedSubmitNodeCollector textCollector = submitNodeCollector.order(BetterDisenchanterConfig.isTextBackgroundEnabled() ? 1 : 0);

            textCollector.submitText(
                    poseStack,
                    x1,
                    -10.0F,
                    state.line1.getVisualOrderText(),
                    dropShadow,
                    Font.DisplayMode.POLYGON_OFFSET,
                    LightCoordsUtil.FULL_BRIGHT,
                    0xFFFFFFFF,
                    0,
                    0
            );

            textCollector.submitText(
                    poseStack,
                    x2,
                    2.0F,
                    state.line2.getVisualOrderText(),
                    dropShadow,
                    Font.DisplayMode.POLYGON_OFFSET,
                    LightCoordsUtil.FULL_BRIGHT,
                    0xFFDDDDDD,
                    0,
                    0
            );

            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(BetterDisenchanterBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.5, pos.getZ() + 1.0);
    }

    private static Component cleanText(Component comp) {
        if (comp == null) return Component.empty();
        String str = comp.getString();
        if (str.contains("\uFE0F") || str.contains("\uFE0E")) {
            return Component.literal(str.replace("\uFE0F", "").replace("\uFE0E", "")).setStyle(comp.getStyle());
        }
        return comp;
    }

    public static class RenderState extends BlockEntityRenderState {
        public boolean hasBook;
        public boolean bookHasFoil;
        public float bookTime;
        public float bookRot;
        public float bookOpenAngle;
        public float bookPageFlip;

        public boolean hasFloatingItem;
        public final ItemStackRenderState floatingItemRenderState = new ItemStackRenderState();
        public float floatingItemGameTime;
        public boolean floatingItemIsEnchanting;
        public float floatingItemShakeProgress;

        public boolean showHud;
        public boolean hasCatalystForY;
        public float billboardGameTime;
        public Component line1 = Component.empty();
        public Component line2 = Component.empty();
        public float w1;
        public float w2;

        public boolean hasCatalystItem;
        public final ItemStackRenderState catalystItemRenderState = new ItemStackRenderState();
        public float catalystGameTime;
        public boolean catalystIsEnchanting;
    }
}
