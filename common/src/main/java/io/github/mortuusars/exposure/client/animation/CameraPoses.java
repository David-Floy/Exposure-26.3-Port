package io.github.mortuusars.exposure.client.animation;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;

public class CameraPoses {
    public void applyHolding(HumanoidModel<?> model, HumanoidArm arm, float actionAnim) {

        boolean rightHanded = arm == HumanoidArm.RIGHT;

        ModelPart mainHand = rightHanded ? model.rightArm : model.leftArm;
        ModelPart offHand = rightHanded ? model.leftArm : model.rightArm;

        model.head.xRot += 0.4f; // Applying part of head rotation. If we turn head down completely - arms will be too low.
        model.head.xRot = Math.clamp(model.head.xRot, -1F, 1.15F); // Look up/down limit

        mainHand.yRot = (rightHanded ? -0.3F : 0.3F) + model.head.yRot;
        offHand.yRot = (rightHanded ? 0.5F : -0.5F) + model.head.yRot;
        mainHand.xRot = model.head.xRot - 1.5F;
        offHand.xRot = model.head.xRot - 1.5F;
        offHand.xRot += (actionAnim * 0.1F) * (rightHanded ? 1 : -1);
        offHand.yRot += (actionAnim * 0.1F) * (rightHanded ? 1 : -1);
        model.head.xRot += 0.3f; // Applying rest of head rotation after arms

        // In 26.1.2 headwear is a child of head and inherits this pose.
        // Copying the parent pose into the child applies the transform twice.
    }

    public void applySelfie(HumanoidModel<?> model, HumanoidArm arm, boolean cameraEntity) {
        ModelPart cameraArm = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;

        // Arm follows camera:
        cameraArm.xRot = (model.head.xRot + Math.abs(model.head.xRot * 0.13f)) + (-(float) Math.PI / 2F);
        cameraArm.yRot = model.head.yRot;
        if (cameraEntity) {
            cameraArm.yRot += (arm == HumanoidArm.RIGHT ? -0.25f : 0.25f);
        }

        if (model.head.xRot <= 0) {
            cameraArm.zRot = (model.head.xRot * 0.15f) * (arm == HumanoidArm.RIGHT ? -1 : 1);
        } else {
            cameraArm.zRot = (model.head.xRot * 0.22f) * (arm == HumanoidArm.RIGHT ? -1 : 1);
        }
    }

    public void applyDisassembled(HumanoidModel<?> model, HumanoidArm arm, float actionAnim) {
        model.head.xRot += 0.4f; // Applying part of head rotation. If we turn head down completely - arms will be too low.
        model.head.xRot = Math.clamp(model.head.xRot, -0.75F, 0.75F); // Look up/down limit

        boolean rightHanded = arm == HumanoidArm.RIGHT;

        ModelPart mainHand = rightHanded ? model.rightArm : model.leftArm;
        ModelPart offHand = rightHanded ? model.leftArm : model.rightArm;
        mainHand.yRot = (rightHanded ? -0.6F : 0.6F) + model.head.yRot;
        offHand.yRot = (rightHanded ? 0.6F : -0.6F) + model.head.yRot;
        mainHand.xRot = model.head.xRot - 1.5F;
        offHand.xRot = model.head.xRot - 1.5F;
        offHand.xRot += (actionAnim * 0.1F) * (rightHanded ? 1 : -1);
        offHand.yRot += (actionAnim * 0.1F) * (rightHanded ? 1 : -1);
        model.head.xRot += 0.3f; // Applying rest of head rotation after arms

        // Headwear inherits the head transform through the 26.1.2 model hierarchy.
    }

    public void applyStand(HumanoidModel<?> model, HumanoidArm arm, float headXRot, float headYRot, float actionAnim) {
        boolean rightHanded = arm == HumanoidArm.RIGHT;

        ModelPart mainHand = rightHanded ? model.rightArm : model.leftArm;
        ModelPart offHand = rightHanded ? model.leftArm : model.rightArm;

        model.head.yRot = headYRot;
        model.head.xRot = headXRot;
        // Headwear inherits the head transform through the 26.1.2 model hierarchy.

        // Arms to stand:
        mainHand.yRot = (rightHanded ? -0.2F : 0.2F) + model.head.yRot;
        offHand.yRot = (rightHanded ? 0.2F : -0.2F) + model.head.yRot;
        mainHand.xRot = -1.2f;
        offHand.xRot = -1.2f;
        offHand.xRot += (actionAnim * 0.1F) * (rightHanded ? 1 : -1);
        offHand.yRot += (actionAnim * 0.1F) * (rightHanded ? 1 : -1);
    }
}
