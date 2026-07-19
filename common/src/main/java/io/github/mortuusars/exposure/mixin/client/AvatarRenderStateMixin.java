package io.github.mortuusars.exposure.mixin.client;

import io.github.mortuusars.exposure.client.render.photograph.AvatarPhotographRenderState;
import io.github.mortuusars.exposure.client.render.photograph.HeldPhotographRenderRequest;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements AvatarPhotographRenderState {
    @Unique private HeldPhotographRenderRequest exposure$rightHandPhotograph = HeldPhotographRenderRequest.EMPTY;
    @Unique private HeldPhotographRenderRequest exposure$leftHandPhotograph = HeldPhotographRenderRequest.EMPTY;

    @Override
    public HeldPhotographRenderRequest exposure$getRightHandPhotograph() {
        return exposure$rightHandPhotograph;
    }

    @Override
    public void exposure$setRightHandPhotograph(HeldPhotographRenderRequest request) {
        exposure$rightHandPhotograph = request;
    }

    @Override
    public HeldPhotographRenderRequest exposure$getLeftHandPhotograph() {
        return exposure$leftHandPhotograph;
    }

    @Override
    public void exposure$setLeftHandPhotograph(HeldPhotographRenderRequest request) {
        exposure$leftHandPhotograph = request;
    }
}
