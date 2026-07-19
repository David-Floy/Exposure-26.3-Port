package io.github.mortuusars.exposure.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.PlatformHelperClient;
import io.github.mortuusars.exposure.client.image.modifier.ImageEffect;
import io.github.mortuusars.exposure.client.image.renderable.RenderableImage;
import io.github.mortuusars.exposure.client.render.image.RenderCoordinates;
import io.github.mortuusars.exposure.client.render.photograph.PhotographStyle;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.entity.PhotographFrameEntity;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.NotNull;

/** Collector-era renderer for the solid photograph frame and its dynamic photo. */
public class PhotographFrameEntityRenderer<T extends PhotographFrameEntity> extends EntityRenderer<T, PhotographFrameRenderState> {
    protected final ItemModelResolver itemModelResolver;

    public PhotographFrameEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    public Identifier getModelLocation(T entity, int size) {
        return switch (size) {
            case 0 -> ExposureClient.Models.PHOTOGRAPH_FRAME_SMALL;
            case 1 -> ExposureClient.Models.PHOTOGRAPH_FRAME_MEDIUM;
            case 2 -> ExposureClient.Models.PHOTOGRAPH_FRAME_LARGE;
            default -> throw new IllegalArgumentException("size " + size + " is not valid. Expected 0-2.");
        };
    }

    protected net.minecraft.client.renderer.rendertype.RenderType getRenderType() {
        return RenderTypes.solidMovingBlock();
    }

    @Override
    public @NotNull PhotographFrameRenderState createRenderState() {
        return new PhotographFrameRenderState();
    }

    @Override
    public void extractRenderState(T entity, PhotographFrameRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.direction = entity.getDirection();
        state.size = entity.getSize();
        state.itemRotation = entity.getItemRotation();
        state.xRot = entity.getXRot();
        state.yRot = entity.getYRot();
        state.frameInvisible = entity.isFrameInvisible();
        state.glowing = entity.isGlowing();
        state.photographLight = state.glowing ? 0xF000F0 : state.lightCoords;

        state.frameModelParts.clear();
        if (!state.frameInvisible) {
            BlockStateModel model = PlatformHelperClient.getModel(getModelLocation(entity, state.size));
            model.collectParts(RandomSource.create(entity.getId()), state.frameModelParts);
        }

        state.image = null;
        state.fallbackItem.clear();
        ItemStack item = entity.getItem();
        if (item.getItem() instanceof PhotographItem photographItem) {
            PhotographStyle style = PhotographStyle.of(item);
            Frame frame = photographItem.getFrame(item);
            RenderableImage image = style.process(ExposureClient.renderedExposures().getOrCreate(frame));
            if (Config.Client.PIXEL_PERFECT_PHOTOGRAPH_FRAME.get()) {
                int pixels = 16 * (state.size + 1) - (state.frameInvisible ? 0 : 4);
                image = image.modifyWith(ImageEffect.Resize.to(pixels)::modify, "pixels-" + pixels);
            }
            if (!image.isEmpty()) {
                int brightness = state.glowing ? 255 : getPhotographBrightness(entity);
                state.image = ExposureClient.imageRenderer().request(image, RenderCoordinates.DEFAULT, state.photographLight,
                        brightness, brightness, brightness, 255);
            }
        }

        if (state.image == null && !item.isEmpty()) {
            this.itemModelResolver.updateForNonLiving(state.fallbackItem, item, ItemDisplayContext.FIXED, entity);
        }
    }

    @Override
    public void submit(PhotographFrameRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraRenderState) {
        super.submit(state, poseStack, collector, cameraRenderState);
        poseStack.pushPose();
        double hangOffset = 0.46875;
        poseStack.translate(state.direction.getStepX() * hangOffset, state.direction.getStepY() * hangOffset, state.direction.getStepZ() * hangOffset);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));

        if (state.image != null) submitPhotograph(state, poseStack, collector);
        else if (!state.fallbackItem.isEmpty()) submitFallbackItem(state, poseStack, collector);
        if (!state.frameInvisible) submitFrame(state, poseStack, collector);
        poseStack.popPose();
    }

    protected void submitFrame(PhotographFrameRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        collector.submitBlockModel(poseStack, getRenderType(), state.frameModelParts, BlockModelRenderState.EMPTY_TINTS,
                state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }

    protected void submitPhotograph(PhotographFrameRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        float border = state.frameInvisible ? 0f : 0.125f;
        float center = (float) ((state.frameInvisible ? 0.497f : 0.48f) - Config.Client.PHOTOGRAPH_FRAME_IMAGE_OFFSET.get());
        float size = state.size + 1 - border * 2;
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.itemRotation * 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(-0.5 * (state.size + 1) + border, -0.5 * (state.size + 1) + border, center);
        poseStack.scale(size, size, 1);
        ExposureClient.imageRenderer().submit(state.image, poseStack, collector);
        poseStack.popPose();
    }

    protected void submitFallbackItem(PhotographFrameRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        float scale = 0.65f + state.size * 0.5f;
        poseStack.translate(0, 0, 0.46875);
        poseStack.scale(scale, scale, scale * 0.75f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.itemRotation * 90.0F));
        state.fallbackItem.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }

    protected int getPhotographBrightness(T entity) {
        if (entity.getDirection() == Direction.UP) return 255;
        int light = entity.level().getBrightness(LightLayer.BLOCK, entity.blockPosition());
        float shade = switch (entity.getDirection()) {
            case DOWN -> 0.5f;
            case NORTH, SOUTH -> 0.8f;
            default -> 1.0f;
        };
        shade += (1f - shade) * 0.2f;
        int shaded = (int) (255 * shade);
        return Math.min(255, shaded + (int) ((255 - shaded) * (light / 15f * 0.5f)));
    }

    @Override
    protected boolean shouldShowName(T entity, double distanceToCameraSq) {
        return Minecraft.renderNames() && !entity.getItem().isEmpty() && entity.getItem().has(DataComponents.CUSTOM_NAME)
                && Minecraft.getInstance().crosshairPickEntity == entity
                && distanceToCameraSq < (entity.isDiscrete() ? 1024.0 : 4096.0);
    }
}
