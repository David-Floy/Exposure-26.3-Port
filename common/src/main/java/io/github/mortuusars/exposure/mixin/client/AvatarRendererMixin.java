package io.github.mortuusars.exposure.mixin.client;

import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.render.photograph.AvatarPhotographRenderState;
import io.github.mortuusars.exposure.client.render.photograph.HeldPhotographRenderRequest;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces only player hand item states whose dynamic photograph request is valid. */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void exposure$extractHeldPhotographs(Avatar entity, AvatarRenderState state, float partialTick,
                                                 CallbackInfo ci) {
        AvatarPhotographRenderState photographState = (AvatarPhotographRenderState) state;
        photographState.exposure$setRightHandPhotograph(HeldPhotographRenderRequest.EMPTY);
        photographState.exposure$setLeftHandPhotograph(HeldPhotographRenderRequest.EMPTY);

        HeldPhotographRenderRequest right = ExposureClient.photographRenderer().prepareHeld(
                state.rightHandItemStack, true, true, state.lightCoords, 255, 255, 255, 255);
        if (!right.isEmpty()) {
            state.rightHandItemState.clear();
            photographState.exposure$setRightHandPhotograph(right);
        }

        HeldPhotographRenderRequest left = ExposureClient.photographRenderer().prepareHeld(
                state.leftHandItemStack, true, true, state.lightCoords, 255, 255, 255, 255);
        if (!left.isEmpty()) {
            state.leftHandItemState.clear();
            photographState.exposure$setLeftHandPhotograph(left);
        }
    }
}
