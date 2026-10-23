package io.github.mortuusars.exposure.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.camera.CameraClient;
import io.github.mortuusars.exposure.client.render.photograph.HeldPhotographRenderRequest;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.StackedPhotographsItem;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3 replaced {@code ItemInHandRenderer#renderArmWithItem} with this state-driven renderer. The held items and
 * the player's visibility now come from the extracted {@link FirstPersonHandsAndItemsRenderState} and
 * {@link PlayerRenderState} instead of renderer fields and the live player.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {
    @Shadow
    protected abstract void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                                            float inverseArmHeight, float attackValue, HumanoidArm arm,
                                            PlayerRenderState playerState);
    @Shadow
    protected abstract float calculateMapTilt(float xRot);
    @Shadow
    protected abstract void renderMapHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                                          HumanoidArm arm, PlayerRenderState playerState);

    @Inject(method = "submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0),
            cancellable = true)
    private void renderPhotograph(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state,
                                  float partialTicks, float xRot, InteractionHand hand, float attack, ItemStack stack,
                                  float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector collector,
                                  int lightCoords, CallbackInfo ci,
                                  @Local boolean isMainHand, @Local HumanoidArm arm) {
        if (CameraClient.viewfinder() != null && CameraClient.viewfinder().isLookingThrough()) {
            poseStack.popPose();
            ci.cancel();
            return;
        }

        if (stack.getItem() instanceof PhotographItem || stack.getItem() instanceof StackedPhotographsItem) {
            HeldPhotographRenderRequest request = ExposureClient.photographRenderer()
                    .prepareHeld(stack, true, false, lightCoords, 255, 255, 255, 255);
            if (request.isEmpty()) return;
            AvatarRenderState avatarState = playerState.avatarRenderState;
            boolean invisible = avatarState == null || avatarState.isInvisible;
            if (isMainHand && state.offHandItem.isEmpty()) {
                exposure$renderTwoHandedPhotograph(playerState, invisible, poseStack, collector, lightCoords, xRot,
                        inverseArmHeight, attack, request);
            } else {
                exposure$renderOneHandedPhotograph(playerState, invisible, poseStack, collector, lightCoords,
                        inverseArmHeight, arm, attack, request);
            }

            poseStack.popPose();

            ci.cancel();
        }
    }

    @Unique
    private void exposure$renderOneHandedPhotograph(PlayerRenderState playerState, boolean invisible, PoseStack poseStack,
                                                     SubmitNodeCollector collector, int packedLight,
                                                     float equippedProgress, HumanoidArm hand, float swingProgress,
                                                     HeldPhotographRenderRequest request) {
        float f = hand == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate(f * 0.125F, -0.125D, 0.0D);
        if (!invisible) {
            poseStack.pushPose();
            poseStack.rotate(Axis.ZP.rotationDegrees(f * 10.0F));
            this.renderPlayerArm(poseStack, collector, packedLight, equippedProgress, swingProgress, hand, playerState);
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.translate(f * 0.51F, -0.08F + equippedProgress * -1.2F, -0.75D);
        float f1 = Mth.sqrt(swingProgress);
        float f2 = Mth.sin(f1 * (float)Math.PI);
        float f3 = -0.5F * f2;
        float f4 = 0.4F * Mth.sin(f1 * ((float)Math.PI * 2F));
        float f5 = -0.3F * Mth.sin(swingProgress * (float)Math.PI);
        poseStack.translate(f * f3, f4 - 0.3F * f2, f5);
        poseStack.rotate(Axis.XP.rotationDegrees(f2 * -45.0F));
        poseStack.rotate(Axis.YP.rotationDegrees(f * f2 * -30.0F));

        poseStack.rotate(Axis.YP.rotationDegrees(180.0F));
        poseStack.rotate(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.38f, 0.38f, 0.38f);
        poseStack.translate(-0.5, -0.5, 0);
        poseStack.scale(1f, 1f, -1f);

        ExposureClient.photographRenderer().submitHeld(request, poseStack, collector);

        poseStack.popPose();
    }

    @Unique
    private void exposure$renderTwoHandedPhotograph(PlayerRenderState playerState, boolean invisible, PoseStack poseStack,
                                                     SubmitNodeCollector collector, int packedLight, float pitch,
                                                     float equippedProgress, float swingProgress,
                                                     HeldPhotographRenderRequest request) {
        float f = Mth.sqrt(swingProgress);
        float f1 = -0.2F * Mth.sin(swingProgress * (float)Math.PI);
        float f2 = -0.4F * Mth.sin(f * (float)Math.PI);
        poseStack.translate(0.0D, -f1 / 2.0F, f2);
        float f3 = this.calculateMapTilt(pitch);
        poseStack.translate(0.0D, 0.04F + equippedProgress * -1.2F + f3 * -0.5F, -0.72F);
        poseStack.rotate(Axis.XP.rotationDegrees(f3 * -85.0F));
        if (!invisible) {
            poseStack.pushPose();
            poseStack.rotate(Axis.YP.rotationDegrees(90.0F));
            this.renderMapHand(poseStack, collector, packedLight, HumanoidArm.RIGHT, playerState);
            this.renderMapHand(poseStack, collector, packedLight, HumanoidArm.LEFT, playerState);
            poseStack.popPose();
        }

        float f4 = Mth.sin(f * (float)Math.PI);
        poseStack.rotate(Axis.XP.rotationDegrees(f4 * 20.0F));
        poseStack.scale(2.0F, 2.0F, 2.0F);

        poseStack.rotate(Axis.YP.rotationDegrees(180.0F));
        poseStack.rotate(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.38f, 0.38f, 0.38f);
        poseStack.translate(-0.5, -0.5, 0);
        poseStack.scale(1f, 1f, -1f);

        ExposureClient.photographRenderer().submitHeld(request, poseStack, collector);
    }
}
