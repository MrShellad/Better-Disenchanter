package com.betterdisenchanter.client;

import com.betterdisenchanter.BetterDisenchanterConfig;
import com.betterdisenchanter.block.entity.BetterDisenchanterBlockEntity;
import com.betterdisenchanter.recipe.CatalystRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;

public class BetterDisenchanterBlockEntityRenderer implements BlockEntityRenderer<BetterDisenchanterBlockEntity> {

    private final BookModel bookModel;
    private final ItemRenderer itemRenderer;
    private final Font font;

    public BetterDisenchanterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
        this.itemRenderer = context.getItemRenderer();
        this.font = context.getFont();
    }

    @Override
    public void render(BetterDisenchanterBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (entity.getLevel() == null) return;

        // 1. Render Book
        if (!entity.getBook().isEmpty()) {
            renderBook(entity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }

        // 2. Render Floating Enchanted Item (above open book)
        if (entity.bookOpenAngle >= 0.8f && !entity.getItem().isEmpty()) {
            renderFloatingItem(entity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }

        // 3. Render High-Version Native Item Display Effect (Centered Catalyst & Text Display Billboard)
        renderDisplayHUD(entity, partialTick, poseStack, bufferSource, packedOverlay);
    }

    private void renderBook(BetterDisenchanterBlockEntity entity, float partialTick, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.75D, 0.5D);
        float gameTime = entity.getLevel().getGameTime() + partialTick;
        poseStack.translate(0.0D, 0.1F + Mth.sin(gameTime * 0.1F) * 0.01F, 0.0D);

        float interpolatedRot = -(entity.bookLastRot + BetterDisenchanterBlockEntity.aroundRadial(entity.bookRot - entity.bookLastRot) * partialTick);
        poseStack.mulPose(Axis.YP.rotation(interpolatedRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));

        VertexConsumer vertexConsumer = EnchantTableRenderer.BOOK_LOCATION.buffer(bufferSource, RenderType::entitySolid);
        if (entity.getBook().is(Items.ENCHANTED_BOOK) || entity.getBook().hasFoil()) {
            vertexConsumer = VertexMultiConsumer.create(bufferSource.getBuffer(RenderType.entityGlint()), vertexConsumer);
        }

        float openAngle = entity.bookLastOpenAngle + (entity.bookOpenAngle - entity.bookLastOpenAngle) * partialTick;
        this.bookModel.setupAnim(1.0F, 0.0F, 0.0F, openAngle);
        this.bookModel.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, -1);

        poseStack.popPose();
    }

    private void renderFloatingItem(BetterDisenchanterBlockEntity entity, float partialTick, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        double itemY = BetterDisenchanterConfig.getFloatingItemYOffset();
        poseStack.translate(0.5D, itemY, 0.5D);

        float gameTime = entity.getLevel().getGameTime() + partialTick;
        float bob = Mth.sin(gameTime * 0.12F) * 0.025F;
        poseStack.translate(0.0D, bob, 0.0D);

        // Smooth rotation
        poseStack.mulPose(Axis.YP.rotationDegrees(gameTime * 2.2F));
        float itemScale = BetterDisenchanterConfig.getFloatingItemScale();
        poseStack.scale(itemScale, itemScale, itemScale);

        itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND, packedLight, packedOverlay,
                poseStack, bufferSource, entity.getLevel(), 0);

        poseStack.popPose();
    }

    private void renderDisplayHUD(BetterDisenchanterBlockEntity entity, float partialTick, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedOverlay) {
        boolean isWaiting = entity.getStatus() == BetterDisenchanterBlockEntity.Status.WAITING;
        boolean isEnchanting = entity.getStatus() == BetterDisenchanterBlockEntity.Status.ENCHANTING;

        if (!isWaiting && !isEnchanting) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

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
        } else if (player.distanceToSqr(entity.getBlockPos().getCenter()) < 36.0D) {
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
                // Empty hand -> preview default disenchanting (if enabled)
                recipe = CatalystRecipe.DEFAULT_PROCESSOR;
                displayStack = ItemStack.EMPTY;
            }
        }

        if (recipe == null) return;

        float gameTime = entity.getLevel().getGameTime() + partialTick;

        // 1. Render 3D Floating Catalyst Item (True Native Item Display, centered at X=0.5, Z=0.5)
        if (!displayStack.isEmpty()) {
            poseStack.pushPose();
            double catalystY = BetterDisenchanterConfig.getCatalystItemYOffset();
            poseStack.translate(0.5D, catalystY, 0.5D);

            float itemBob = Mth.sin(gameTime * 0.12F) * 0.02F;
            poseStack.translate(0.0D, itemBob, 0.0D);

            float rotSpeed = isEnchanting ? 6.0F : 3.0F;
            poseStack.mulPose(Axis.YP.rotationDegrees(gameTime * rotSpeed));
            float catalystScale = BetterDisenchanterConfig.getCatalystItemScale();
            poseStack.scale(catalystScale, catalystScale, catalystScale);

            itemRenderer.renderStatic(displayStack, ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT, packedOverlay,
                    poseStack, bufferSource, entity.getLevel(), 0);
            poseStack.popPose();
        }

        // 2. Render Text Display Billboard (Centered at X=0.5, Z=0.5, directly above catalyst with 1-line spacing)
        poseStack.pushPose();
        double billboardY = BetterDisenchanterConfig.getDisplayBillboardYOffset();
        double textY = displayStack.isEmpty() ? (billboardY - 0.65D) : billboardY;
        poseStack.translate(0.5D, textY, 0.5D);

        float textBob = Mth.sin(gameTime * 0.08F) * 0.015F;
        poseStack.translate(0.0D, textBob, 0.0D);

        // Billboard facing camera
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        // Scale with positive X so quads are not mirrored/culled!
        float textScale = BetterDisenchanterConfig.getTextDisplayScale();
        poseStack.scale(textScale, -textScale, textScale);

        Matrix4f matrix = poseStack.last().pose();

        // Build Title Component
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

        // Build Action Component
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

        Component line1 = cleanText(Component.empty().append(titleComponent).append(" ➔ ").append(resultBadge));
        Component line2 = cleanText(actionComponent);

        // Horizontally center both lines at X=0
        float w1 = font.width(line1);
        float w2 = font.width(line2);
        float x1 = -w1 / 2.0F;
        float x2 = -w2 / 2.0F;

        // Draw unified semi-transparent dark background plate behind both lines (if enabled in config)
        if (BetterDisenchanterConfig.isTextBackgroundEnabled()) {
            float halfW = Math.max(w1, w2) / 2.0F + 4.0F;
            float minX = -halfW;
            float maxX = halfW;
            float minY = -13.0F;
            float maxY = 13.0F;

            VertexConsumer bgConsumer = bufferSource.getBuffer(RenderType.textBackground());
            bgConsumer.addVertex(matrix, minX, minY, -0.01F).setColor(0x80000000).setLight(LightTexture.FULL_BRIGHT);
            bgConsumer.addVertex(matrix, minX, maxY, -0.01F).setColor(0x80000000).setLight(LightTexture.FULL_BRIGHT);
            bgConsumer.addVertex(matrix, maxX, maxY, -0.01F).setColor(0x80000000).setLight(LightTexture.FULL_BRIGHT);
            bgConsumer.addVertex(matrix, maxX, minY, -0.01F).setColor(0x80000000).setLight(LightTexture.FULL_BRIGHT);
        }

        boolean dropShadow = BetterDisenchanterConfig.isTextDropShadowEnabled();

        // Draw Line 1 with POLYGON_OFFSET and backgroundColor=0 (prevents Z-fighting and font-internal clipping)
        font.drawInBatch(line1, x1, -10.0F, 0xFFFFFFFF, dropShadow, matrix, bufferSource,
                Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);

        // Draw Line 2 with POLYGON_OFFSET
        font.drawInBatch(line2, x2, 2.0F, 0xFFDDDDDD, dropShadow, matrix, bufferSource,
                Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
    }

    private static Component cleanText(Component comp) {
        if (comp == null) return Component.empty();
        String str = comp.getString();
        if (str.contains("\uFE0F") || str.contains("\uFE0E")) {
            return Component.literal(str.replace("\uFE0F", "").replace("\uFE0E", "")).setStyle(comp.getStyle());
        }
        return comp;
    }
}
