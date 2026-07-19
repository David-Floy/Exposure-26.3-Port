package io.github.mortuusars.exposure.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.camera.CameraClient;
import io.github.mortuusars.exposure.client.render.photograph.HeldPhotographRenderRequest;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.StackedPhotographsItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
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

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow
    private ItemStack mainHandItem;
    @Shadow
    private ItemStack offHandItem;
    @Shadow
    protected abstract void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int combinedLight,
                                            float equippedProgress, float swingProgress, HumanoidArm side);
    @Shadow
    protected abstract float calculateMapTilt(float pPitch);
    @Shadow
    protected abstract void renderMapHand(PoseStack poseStack, SubmitNodeCollector collector, int combinedLight, HumanoidArm side);

    @Inject(method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0),
            cancellable = true)
    private void renderPhotograph(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand,
                                  float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack,
                                  SubmitNodeCollector collector, int combinedLight, CallbackInfo ci,
                                  @Local boolean isMainHand, @Local HumanoidArm arm) {
        if (CameraClient.viewfinder() != null && CameraClient.viewfinder().isLookingThrough()) {
            poseStack.popPose();
            ci.cancel();
            return;
        }

        if (stack.getItem() instanceof PhotographItem || stack.getItem() instanceof StackedPhotographsItem) {
            HeldPhotographRenderRequest request = ExposureClient.photographRenderer()
                    .prepareHeld(stack, true, false, combinedLight, 255, 255, 255, 255);
            if (request.isEmpty()) return;
            if (isMainHand && this.offHandItem.isEmpty()) {
                exposure$renderTwoHandedPhotograph(player, poseStack, collector, combinedLight, pitch,
                        equipProgress, swingProgress, request);
            } else {
                exposure$renderOneHandedPhotograph(player, poseStack, collector, combinedLight, equipProgress,
                        arm, swingProgress, request);
            }

            poseStack.popPose();

            ci.cancel();
        }
    }

    @Unique
    private void exposure$renderOneHandedPhotograph(AbstractClientPlayer player, PoseStack poseStack,
                                                     SubmitNodeCollector collector, int packedLight,
                                                     float equippedProgress, HumanoidArm hand, float swingProgress,
                                                     HeldPhotographRenderRequest request) {
        float f = hand == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate(f * 0.125F, -0.125D, 0.0D);
        if (!player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(f * 10.0F));
            this.renderPlayerArm(poseStack, collector, packedLight, equippedProgress, swingProgress, hand);
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
        poseStack.mulPose(Axis.XP.rotationDegrees(f2 * -45.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(f * f2 * -30.0F));

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.38f, 0.38f, 0.38f);
        poseStack.translate(-0.5, -0.5, 0);
        poseStack.scale(1f, 1f, -1f);

        ExposureClient.photographRenderer().submitHeld(request, poseStack, collector);

        poseStack.popPose();
    }

    @Unique
    private void exposure$renderTwoHandedPhotograph(AbstractClientPlayer player, PoseStack poseStack,
                                                     SubmitNodeCollector collector, int packedLight, float pitch,
                                                     float equippedProgress, float swingProgress,
                                                     HeldPhotographRenderRequest request) {
        float f = Mth.sqrt(swingProgress);
        float f1 = -0.2F * Mth.sin(swingProgress * (float)Math.PI);
        float f2 = -0.4F * Mth.sin(f * (float)Math.PI);
        poseStack.translate(0.0D, -f1 / 2.0F, f2);
        float f3 = this.calculateMapTilt(pitch);
        poseStack.translate(0.0D, 0.04F + equippedProgress * -1.2F + f3 * -0.5F, -0.72F);
        poseStack.mulPose(Axis.XP.rotationDegrees(f3 * -85.0F));
        if (!player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            this.renderMapHand(poseStack, collector, packedLight, HumanoidArm.RIGHT);
            this.renderMapHand(poseStack, collector, packedLight, HumanoidArm.LEFT);
            poseStack.popPose();
        }

        float f4 = Mth.sin(f * (float)Math.PI);
        poseStack.mulPose(Axis.XP.rotationDegrees(f4 * 20.0F));
        poseStack.scale(2.0F, 2.0F, 2.0F);

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.38f, 0.38f, 0.38f);
        poseStack.translate(-0.5, -0.5, 0);
        poseStack.scale(1f, 1f, -1f);

        ExposureClient.photographRenderer().submitHeld(request, poseStack, collector);
    }
}
