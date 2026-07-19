package io.github.mortuusars.exposure.client.animation;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import org.jetbrains.annotations.Nullable;

/** Entity-free snapshot consumed by HumanoidModel during setupAnim. */
public record CameraPoseRenderState(Pose pose, @Nullable Identifier cameraItemId, HumanoidArm arm,
                                    float actionAnim, boolean cameraEntity,
                                    float standHeadXRot, float standHeadYRot) {
    public static final CameraPoseRenderState EMPTY = new CameraPoseRenderState(
            Pose.NONE, null, HumanoidArm.RIGHT, 0, false, 0, 0);

    public enum Pose {
        NONE,
        HOLDING,
        SELFIE,
        DISASSEMBLED,
        STAND
    }
}
