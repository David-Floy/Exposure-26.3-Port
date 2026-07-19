package io.github.mortuusars.exposure.mixin.client;

import io.github.mortuusars.exposure.client.animation.CameraPoseRenderState;
import io.github.mortuusars.exposure.client.animation.ExposureHumanoidRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HumanoidRenderState.class)
public class HumanoidRenderStateMixin implements ExposureHumanoidRenderState {
    @Unique
    private CameraPoseRenderState exposure$cameraPose = CameraPoseRenderState.EMPTY;

    @Override
    public CameraPoseRenderState exposure$getCameraPose() {
        return exposure$cameraPose;
    }

    @Override
    public void exposure$setCameraPose(CameraPoseRenderState pose) {
        exposure$cameraPose = pose;
    }
}
