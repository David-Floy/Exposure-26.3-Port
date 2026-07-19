package io.github.mortuusars.exposure.mixin.client;

import io.github.mortuusars.exposure.client.animation.CameraModelPoses;
import io.github.mortuusars.exposure.client.animation.CameraPoseRenderState;
import io.github.mortuusars.exposure.client.animation.CameraPoses;
import io.github.mortuusars.exposure.client.animation.ExposureHumanoidRenderState;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightArm;

    // Applying reduced bobbing directly avoids the arm jitter produced by undoing it afterwards.
    @Unique private float exposure$leftArmBobbingMultiplier = 1F;
    @Unique private float exposure$rightArmBobbingMultiplier = 1F;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/model/HumanoidModel$ArmPose;SPYGLASS:Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
                    opcode = Opcodes.GETSTATIC, ordinal = 0))
    private void exposure$applyCameraPose(HumanoidRenderState state, CallbackInfo ci) {
        exposure$leftArmBobbingMultiplier = 1F;
        exposure$rightArmBobbingMultiplier = 1F;

        CameraPoseRenderState cameraPose = ((ExposureHumanoidRenderState) state).exposure$getCameraPose();
        if (cameraPose.pose() == CameraPoseRenderState.Pose.NONE || cameraPose.cameraItemId() == null) return;

        CameraPoses poses = CameraModelPoses.get(cameraPose.cameraItemId());
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        switch (cameraPose.pose()) {
            case HOLDING -> {
                poses.applyHolding(model, cameraPose.arm(), cameraPose.actionAnim());
                exposure$leftArmBobbingMultiplier = cameraPose.arm() == net.minecraft.world.entity.HumanoidArm.LEFT ? 0.1F : 0.5F;
                exposure$rightArmBobbingMultiplier = cameraPose.arm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 0.1F : 0.5F;
            }
            case SELFIE -> {
                poses.applySelfie(model, cameraPose.arm(), cameraPose.cameraEntity());
                if (cameraPose.arm() == net.minecraft.world.entity.HumanoidArm.LEFT) {
                    exposure$leftArmBobbingMultiplier = 0F;
                } else {
                    exposure$rightArmBobbingMultiplier = 0F;
                }
            }
            case DISASSEMBLED -> poses.applyDisassembled(model, cameraPose.arm(), cameraPose.actionAnim());
            case STAND -> poses.applyStand(model, cameraPose.arm(), cameraPose.standHeadXRot(),
                    cameraPose.standHeadYRot(), cameraPose.actionAnim());
            case NONE -> { }
        }
    }

    @Redirect(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/AnimationUtils;bobModelPart(Lnet/minecraft/client/model/geom/ModelPart;FF)V"))
    private void exposure$adjustArmBobbing(ModelPart modelPart, float ageInTicks, float multiplier) {
        if (exposure$leftArmBobbingMultiplier != 1F && modelPart == leftArm) {
            multiplier = exposure$leftArmBobbingMultiplier * -1;
        }
        if (exposure$rightArmBobbingMultiplier != 1F && modelPart == rightArm) {
            multiplier = exposure$rightArmBobbingMultiplier;
        }
        AnimationUtils.bobModelPart(modelPart, ageInTicks, multiplier);
    }

}
