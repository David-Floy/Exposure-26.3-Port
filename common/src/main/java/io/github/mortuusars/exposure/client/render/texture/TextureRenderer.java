package io.github.mortuusars.exposure.client.render.texture;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.image.renderable.RenderableImageIdentifier;
import io.github.mortuusars.exposure.client.render.image.ImageRenderRequest;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;

/** World textured-quad submission shared by photograph paper and overlays. */
public final class TextureRenderer {
    private TextureRenderer() {}

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, Identifier texture,
                              int packedLight, int red, int green, int blue, int alpha) {
        submit(poseStack, collector, texture, 0, 0, 1, 1, 0, 0, 1, 1, packedLight, red, green, blue, alpha);
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, Identifier texture,
                              float minX, float minY, float maxX, float maxY,
                              float minU, float minV, float maxU, float maxV,
                              int packedLight, int red, int green, int blue, int alpha) {
        ImageRenderRequest request = new ImageRenderRequest(new RenderableImageIdentifier("-texture"),
                minX, minY, maxX, maxY, minU, minV, maxU, maxV, packedLight, red, green, blue, alpha);
        ExposureClient.imageRenderer().submitTexture(texture, request, poseStack, collector);
    }
}
