package io.github.mortuusars.exposure.client.render.photograph;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhotographRenderTransformsTests {
    private static final float EPSILON = 0.00001f;

    @Test
    void backsideFlipsTheUnitQuadAroundItsCentreWithoutTranslatingItsBounds() {
        Matrix4f transform = backsideTransform(0);

        assertPoint(transform, 0, 0, 0, 1, 0, -PhotographRenderTransforms.BACKSIDE_DEPTH);
        assertPoint(transform, 1, 0, 0, 0, 0, -PhotographRenderTransforms.BACKSIDE_DEPTH);
        assertPoint(transform, 0, 1, 0, 1, 1, -PhotographRenderTransforms.BACKSIDE_DEPTH);
        assertPoint(transform, 1, 1, 0, 0, 1, -PhotographRenderTransforms.BACKSIDE_DEPTH);
    }

    @Test
    void everyPaperRotationKeepsTheBacksideCentredAndFacingAway() {
        for (int rotation : new int[] {0, 90, 180, 270}) {
            Matrix4f transform = backsideTransform(rotation);
            Vector3f centre = transform.transformPosition(new Vector3f(0.5f, 0.5f, 0));
            Vector3f normal = transform.transformDirection(new Vector3f(0, 0, 1));

            assertEquals(0.5f, centre.x, EPSILON, "rotation " + rotation + " must preserve the X centre");
            assertEquals(0.5f, centre.y, EPSILON, "rotation " + rotation + " must preserve the Y centre");
            assertEquals(-PhotographRenderTransforms.BACKSIDE_DEPTH, centre.z, EPSILON,
                    "rotation " + rotation + " must keep the backside directly behind the paper");
            assertEquals(0, normal.x, EPSILON);
            assertEquals(0, normal.y, EPSILON);
            assertEquals(-1, normal.z, EPSILON, "the backside normal must face away from the front");
        }
    }

    private static void assertPoint(Matrix4f transform, float x, float y, float z,
                                    float expectedX, float expectedY, float expectedZ) {
        Vector3f actual = transform.transformPosition(new Vector3f(x, y, z));
        assertEquals(expectedX, actual.x, EPSILON);
        assertEquals(expectedY, actual.y, EPSILON);
        assertEquals(expectedZ, actual.z, EPSILON);
    }

    private static Matrix4f backsideTransform(int paperRotation) {
        PoseStack poseStack = new PoseStack();
        PhotographRenderTransforms.applyBackside(poseStack, paperRotation);
        return new Matrix4f(poseStack.last().pose());
    }
}
