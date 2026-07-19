package io.github.mortuusars.exposure.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.render.photograph.AvatarPhotographRenderState;
import io.github.mortuusars.exposure.client.render.photograph.HeldPhotographRenderRequest;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Submits a dynamic photograph at vanilla's third-person hand-item boundary. */
@Mixin(PlayerItemInHandLayer.class)
public abstract class PlayerItemInHandLayerMixin {
    @SuppressWarnings("unchecked")
    @Inject(method = "submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD"), cancellable = true)
    private void exposure$submitHeldPhotograph(AvatarRenderState state, ItemStackRenderState item,
                                               ItemStack itemStack, HumanoidArm arm, PoseStack poseStack,
                                               SubmitNodeCollector collector, int lightCoords, CallbackInfo ci) {
        HeldPhotographRenderRequest request = ((AvatarPhotographRenderState) state).exposure$getHandPhotograph(arm);
        if (request.isEmpty()) return;

        poseStack.pushPose();
        EntityModel<AvatarRenderState> parentModel =
                (EntityModel<AvatarRenderState>) ((PlayerItemInHandLayer<?, ?>) (Object) this).getParentModel();
        ((ArmedModel<AvatarRenderState>) parentModel).translateToHand(state, arm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        poseStack.mulPose(Axis.YP.rotationDegrees(180));

        boolean babyOffset = state.isBaby && state.entityType != EntityType.ARMOR_STAND;
        float offsetX = babyOffset ? 0 : 1;
        float offsetY = babyOffset ? 1 : 2;
        float offsetZ = babyOffset ? -4.5f : -10;
        poseStack.translate((arm == HumanoidArm.LEFT ? -1 : 1) * offsetX / 16f,
                offsetY / 16f, offsetZ / 16f);

        if (state.attackTime > 0 && state.attackArm == arm && state.swingAnimationType == SwingAnimationType.STAB) {
            SpearAnimations.thirdPersonAttackItem(state, poseStack);
        }
        float ticksUsingItem = state.ticksUsingItem(arm);
        if (ticksUsingItem != 0) {
            HumanoidModel.ArmPose armPose = arm == HumanoidArm.RIGHT ? state.rightArmPose : state.leftArmPose;
            armPose.animateUseItem(state, poseStack, ticksUsingItem, arm, itemStack);
        }

        // exposure:item/photograph inherits minecraft:item/generated. These
        // are its exact default third-person translation/scale plus centering.
        poseStack.translate(0, 3f / 16f, 1f / 16f);
        poseStack.scale(0.55f, 0.55f, 0.55f);
        poseStack.translate(-0.5f, -0.5f, 0);
        ExposureClient.photographRenderer().submitHeld(request, poseStack, collector);
        poseStack.popPose();
        ci.cancel();
    }
}
