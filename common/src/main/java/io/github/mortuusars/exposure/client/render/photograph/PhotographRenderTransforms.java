package io.github.mortuusars.exposure.client.render.photograph;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * Local unit-quad transforms shared by the submitted photograph faces.
 *
 * <p>The renderer's quad spans {@code [0, 1]} on both X and Y.  A back face
 * must therefore turn around that quad's centre, rather than around the
 * caller's origin.  This small shared operation is directly testable without
 * a live render collector.</p>
 */
final class PhotographRenderTransforms {
    static final float BACKSIDE_DEPTH = 0.001f;
    private static final float HALF_QUAD = 0.5f;

    private PhotographRenderTransforms() {
    }

    static void applyBackside(PoseStack poseStack, int paperRotation) {
        poseStack.translate(HALF_QUAD, HALF_QUAD, 0);
        poseStack.rotate(Axis.YP.rotationDegrees(180));
        // After the Y flip, positive local depth is behind the front face.
        poseStack.translate(-HALF_QUAD, -HALF_QUAD, BACKSIDE_DEPTH);
        applyPaperRotation(poseStack, paperRotation);
    }

    static void applyPaperRotation(PoseStack poseStack, int paperRotation) {
        poseStack.translate(HALF_QUAD, HALF_QUAD, 0);
        poseStack.rotate(Axis.ZP.rotationDegrees(paperRotation));
        poseStack.translate(-HALF_QUAD, -HALF_QUAD, 0);
    }
}
