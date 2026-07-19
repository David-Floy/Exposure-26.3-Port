package io.github.mortuusars.exposure.mixin.client;

import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.client.animation.CameraPoseRenderState;
import io.github.mortuusars.exposure.client.animation.EasingFunction;
import io.github.mortuusars.exposure.client.animation.ExposureHumanoidRenderState;
import io.github.mortuusars.exposure.world.camera.Camera;
import io.github.mortuusars.exposure.world.camera.CameraInHand;
import io.github.mortuusars.exposure.world.camera.CameraOnStand;
import io.github.mortuusars.exposure.world.entity.CameraHolder;
import io.github.mortuusars.exposure.world.entity.CameraOperator;
import io.github.mortuusars.exposure.world.entity.CameraStandEntity;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extracts only the scalar/enum data required by the client model pose. */
@Mixin(HumanoidMobRenderer.class)
public class HumanoidMobRendererMixin {
    @Inject(method = "extractHumanoidRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FLnet/minecraft/client/renderer/item/ItemModelResolver;)V",
            at = @At("TAIL"))
    private static void exposure$extractCameraPose(LivingEntity entity, HumanoidRenderState state, float partialTick,
                                                   ItemModelResolver itemModelResolver, CallbackInfo ci) {
        ExposureHumanoidRenderState exposureState = (ExposureHumanoidRenderState) state;
        exposureState.exposure$setCameraPose(CameraPoseRenderState.EMPTY);
        if (!(entity instanceof CameraOperator operator)) return;

        Minecraft minecraft = Minecraft.getInstance();
        Camera activeCamera = operator.getActiveExposureCamera();
        if (activeCamera != null) {
            ItemStack stack = activeCamera.getItemStack();
            if (!(stack.getItem() instanceof CameraItem item)) return;

            HumanoidArm arm = exposure$cameraArm(activeCamera, minecraft);
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            float actionAnim = exposure$actionAnim(operator, partialTick);

            if (activeCamera instanceof CameraOnStand cameraOnStand) {
                CameraStandEntity stand = cameraOnStand.getStand();
                Vec3 direction = entity.getEyePosition().subtract(stand.getEyePosition());
                float yawToStandDegrees = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(direction.x, direction.z)) + 180);
                float yawDegrees = Mth.wrapDegrees(entity.yBodyRot + yawToStandDegrees);
                float headYRot = -(float) Math.toRadians(Mth.clamp(yawDegrees, -60, 60));
                double distanceXZ = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
                float headXRot = -(float) Math.atan2(-direction.y, distanceXZ);
                exposureState.exposure$setCameraPose(new CameraPoseRenderState(
                        CameraPoseRenderState.Pose.STAND, itemId, arm, actionAnim,
                        false, headXRot, headYRot));
                return;
            }

            if (item.isInSelfieMode(stack)) {
                exposureState.exposure$setCameraPose(new CameraPoseRenderState(
                        CameraPoseRenderState.Pose.SELFIE, itemId, arm, actionAnim,
                        minecraft.getCameraEntity() == entity, 0, 0));
                return;
            }

            // Preserve the existing Real Camera compatibility boundary without
            // storing the entity or invoking compatibility code during setupAnim.
            if (minecraft.player == entity && PlatformHelper.isModLoaded("realcamera")) return;

            exposureState.exposure$setCameraPose(new CameraPoseRenderState(
                    CameraPoseRenderState.Pose.HOLDING, itemId, arm, actionAnim,
                    false, 0, 0));
            return;
        }

        if (!(entity instanceof CameraHolder holder)) return;
        CameraInHand camera = CameraInHand.find(holder);
        if (camera == null) return;
        ItemStack stack = camera.getItemStack();
        if (!(stack.getItem() instanceof CameraItem item) || !item.isDisassembled(stack)) return;
        if (minecraft.player == entity && minecraft.options.getCameraType() == CameraType.FIRST_PERSON) return;

        HumanoidArm arm = exposure$cameraArm(camera, minecraft);
        exposureState.exposure$setCameraPose(new CameraPoseRenderState(
                CameraPoseRenderState.Pose.DISASSEMBLED, BuiltInRegistries.ITEM.getKey(item), arm,
                exposure$actionAnim(operator, partialTick), false, 0, 0));
    }

    @Unique
    private static HumanoidArm exposure$cameraArm(Camera camera, Minecraft minecraft) {
        HumanoidArm mainArm = minecraft.options.mainHand().get();
        return camera instanceof CameraInHand inHand && inHand.getHand() == InteractionHand.OFF_HAND
                ? mainArm.getOpposite()
                : mainArm;
    }

    @Unique
    private static float exposure$actionAnim(CameraOperator operator, float partialTick) {
        float progress = operator.getExposureCameraActionAnim(partialTick);
        progress = (float) EasingFunction.EASE_OUT_CUBIC.ease(progress);
        return progress > 0.5F ? 1F - progress : progress;
    }

}
