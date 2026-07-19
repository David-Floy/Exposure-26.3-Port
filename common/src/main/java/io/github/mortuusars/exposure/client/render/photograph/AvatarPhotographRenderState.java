package io.github.mortuusars.exposure.client.render.photograph;

import net.minecraft.world.entity.HumanoidArm;

public interface AvatarPhotographRenderState {
    HeldPhotographRenderRequest exposure$getRightHandPhotograph();
    void exposure$setRightHandPhotograph(HeldPhotographRenderRequest request);
    HeldPhotographRenderRequest exposure$getLeftHandPhotograph();
    void exposure$setLeftHandPhotograph(HeldPhotographRenderRequest request);

    default HeldPhotographRenderRequest exposure$getHandPhotograph(HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? exposure$getRightHandPhotograph() : exposure$getLeftHandPhotograph();
    }
}
