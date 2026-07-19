package io.github.mortuusars.exposure.client.render.image;

import io.github.mortuusars.exposure.client.image.renderable.RenderableImageIdentifier;

/** Lightweight render-state handle; the image pixels stay in ImageRenderer's cache. */
public record ImageRenderRequest(RenderableImageIdentifier imageId,
                                 float minX, float minY, float maxX, float maxY,
                                 float minU, float minV, float maxU, float maxV,
                                 int packedLight, int red, int green, int blue, int alpha) {
}
