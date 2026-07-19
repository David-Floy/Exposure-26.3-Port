package io.github.mortuusars.exposure.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.PlatformHelperClient;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.world.entity.CameraStandEntity;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CameraStandEntityRenderer<T extends CameraStandEntity> extends EntityRenderer<T, CameraStandRenderState> {
    public static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/item/camera.png");
    public static final float MOUNT_SCALE = 0.9f;

    protected final ItemModelResolver itemModelResolver;

    public CameraStandEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public @NotNull CameraStandRenderState createRenderState() {
        return new CameraStandRenderState();
    }

    @Override
    public void extractRenderState(T entity, CameraStandRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        state.hasVehicle = entity.getVehicle() != null;
        state.vehicleYaw = state.hasVehicle
                ? Mth.rotLerp(partialTick, entity.getVehicle().yRotO, entity.getVehicle().getYRot()) : 0.0F;
        state.malfunctioned = entity.isMalfunctioned();

        float hurtTime = (float) entity.getHurtTime() - partialTick;
        float damage = Math.max(0.0F, entity.getDamage() - partialTick);
        state.hurtRotation = hurtTime > 0.0F
                ? Mth.sin(hurtTime) * hurtTime * damage / 10.0F * entity.getHurtDir() : 0.0F;

        state.standModelParts.clear();
        PlatformHelperClient.getModel(ExposureClient.Models.CAMERA_STAND)
                .collectParts(RandomSource.create(entity.getId()), state.standModelParts);
        state.mountModelParts.clear();
        PlatformHelperClient.getModel(ExposureClient.Models.CAMERA_STAND_MOUNT)
                .collectParts(RandomSource.create(entity.getId()), state.mountModelParts);

        ItemStack camera = entity.getCamera();
        state.camera.clear();
        if (!camera.isEmpty()) {
            this.itemModelResolver.updateForNonLiving(state.camera, camera, ItemDisplayContext.NONE, entity);
            state.cameraScale = camera.getItem() instanceof CameraItem cameraItem ? cameraItem.getScaleOnStand() : MOUNT_SCALE;
        }
    }

    @Override
    public void submit(CameraStandRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraRenderState) {
        super.submit(state, poseStack, collector, cameraRenderState);

        if (state.hurtRotation != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(state.hurtRotation));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.hurtRotation));
        }

        renderStand(state, poseStack, collector);
        renderMount(state, poseStack, collector);
        if (!state.camera.isEmpty()) {
            renderCamera(state, poseStack, collector);
        }
    }

    private void renderStand(CameraStandRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();

        if (state.hasVehicle) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.vehicleYaw + 45));
        }

        poseStack.translate(-0.5f, 0f, -0.5f);

        collector.submitBlockModel(poseStack, RenderTypes.solidMovingBlock(), state.standModelParts,
                BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }

    private void renderMount(CameraStandRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        poseStack.translate(0, 1.125, 0);
        float scale = MOUNT_SCALE;
        poseStack.scale(scale, scale, scale);

        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw + 180));
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch));

        if (state.malfunctioned) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-50));
            poseStack.mulPose(Axis.XP.rotationDegrees(-10));
        }

        poseStack.translate(-0.5f, 0f, -0.5f);
        collector.submitBlockModel(poseStack, RenderTypes.solidMovingBlock(), state.mountModelParts,
                BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }

    private void renderCamera(CameraStandRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();

        poseStack.translate(0, 1.125, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw + 180));
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch));
        poseStack.translate(0, 0.125 * MOUNT_SCALE, 0);

        if (state.malfunctioned) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-50));
            poseStack.mulPose(Axis.XP.rotationDegrees(-15));
        }

        poseStack.scale(state.cameraScale, state.cameraScale, state.cameraScale);
        poseStack.translate(0, 0.5, 0);

        state.camera.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
