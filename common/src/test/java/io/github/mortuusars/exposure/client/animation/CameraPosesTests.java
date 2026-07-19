package io.github.mortuusars.exposure.client.animation;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.HumanoidArm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CameraPosesTests {
    private static final float EPSILON = 0.000001F;

    @Test
    void selfiePoseTracksHeadPitchYawAndSelectedRightArm() {
        HumanoidModel<?> model = createModel();
        model.head.xRot = 0.3F;
        model.head.yRot = -0.2F;

        new CameraPoses().applySelfie(model, HumanoidArm.RIGHT, true);

        assertEquals(0.3F + Math.abs(0.3F * 0.13F) - (float) Math.PI / 2F,
                model.rightArm.xRot, EPSILON);
        assertEquals(-0.45F, model.rightArm.yRot, EPSILON);
        assertEquals(-0.3F * 0.22F, model.rightArm.zRot, EPSILON);
        assertEquals(0F, model.leftArm.xRot, EPSILON);
        assertEquals(0F, model.leftArm.yRot, EPSILON);
        assertEquals(0F, model.leftArm.zRot, EPSILON);
    }

    @Test
    void selfiePoseTracksHeadPitchYawAndSelectedLeftArm() {
        HumanoidModel<?> model = createModel();
        model.head.xRot = -0.4F;
        model.head.yRot = 0.35F;

        new CameraPoses().applySelfie(model, HumanoidArm.LEFT, false);

        assertEquals(-0.4F + Math.abs(-0.4F * 0.13F) - (float) Math.PI / 2F,
                model.leftArm.xRot, EPSILON);
        assertEquals(0.35F, model.leftArm.yRot, EPSILON);
        assertEquals(-0.4F * 0.15F, model.leftArm.zRot, EPSILON);
        assertEquals(0F, model.rightArm.xRot, EPSILON);
        assertEquals(0F, model.rightArm.yRot, EPSILON);
        assertEquals(0F, model.rightArm.zRot, EPSILON);
    }

    @Test
    void cameraPosesLeaveChildHeadwearLocalTransformUntouched() {
        CameraPoses poses = new CameraPoses();

        HumanoidModel<?> holding = createModel();
        assertSame(holding.head.getChild("hat"), holding.hat);
        holding.head.xRot = 0.2F;
        holding.head.yRot = -0.3F;
        poses.applyHolding(holding, HumanoidArm.RIGHT, 0F);
        assertLocalHeadwearPoseIsZero(holding.hat);

        HumanoidModel<?> disassembled = createModel();
        disassembled.head.xRot = -0.25F;
        disassembled.head.yRot = 0.4F;
        poses.applyDisassembled(disassembled, HumanoidArm.LEFT, 0F);
        assertLocalHeadwearPoseIsZero(disassembled.hat);

        HumanoidModel<?> stand = createModel();
        poses.applyStand(stand, HumanoidArm.RIGHT, 0.45F, -0.55F, 0F);
        assertEquals(0.45F, stand.head.xRot, EPSILON);
        assertEquals(-0.55F, stand.head.yRot, EPSILON);
        assertLocalHeadwearPoseIsZero(stand.hat);
    }

    private static HumanoidModel<?> createModel() {
        ModelPart root = LayerDefinition.create(
                HumanoidModel.createMesh(CubeDeformation.NONE, 0F), 64, 64).bakeRoot();
        return new HumanoidModel<>(root);
    }

    private static void assertLocalHeadwearPoseIsZero(ModelPart hat) {
        assertEquals(0F, hat.xRot, EPSILON);
        assertEquals(0F, hat.yRot, EPSILON);
        assertEquals(0F, hat.zRot, EPSILON);
        assertEquals(0F, hat.x, EPSILON);
        assertEquals(0F, hat.y, EPSILON);
        assertEquals(0F, hat.z, EPSILON);
    }
}
