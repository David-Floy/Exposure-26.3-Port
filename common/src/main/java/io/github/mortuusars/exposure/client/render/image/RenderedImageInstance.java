package io.github.mortuusars.exposure.client.render.image;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mortuusars.exposure.client.image.renderable.RenderableImage;
import io.github.mortuusars.exposure.util.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Credits to <a href="https://github.com/Jalvaviel/MapMipMapMod">MapMipMapMod by Jalvaviel</a> for example of mipmap implementation for dynamic images.
 * And to <a href="https://github.com/bravely-beep">bravely-beep</a> for pointing me to it.
 */
public class RenderedImageInstance implements AutoCloseable {
    protected final Identifier textureLocation;
    protected RenderableImage image;
    protected MipmappedDynamicTexture texture;
    protected boolean requiresUpload = true;
    private boolean guiUploadQueued;

    RenderedImageInstance(RenderableImage image) {
        this.image = image;
        this.textureLocation = image.getIdentifier().toResourceLocation();
    }

    public void replaceData(RenderableImage image) {
        boolean hasChanged = !image.getIdentifier().equals(this.image.getIdentifier());
        this.image = image;
        if (hasChanged || texture == null || texture.width() != image.width() || texture.height() != image.height()) forceUpload();
    }

    public void forceUpload() {
        this.requiresUpload = true;
    }

    protected void updateTexture() {
        ensureTexture();
        if (texture.pixels() == null) return;

        int width = this.image.width();
        int height = this.image.height();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int ARGB = this.image.getPixelARGB(x, y);
                this.texture.pixels().setPixelABGR(x, y, Color.ABGRtoARGB(ARGB)); // Texture is in ABGR format
            }
        }

        this.texture.upload();
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector collector, ImageRenderRequest request) {
        if (this.requiresUpload) {
            this.updateTexture();
            this.requiresUpload = false;
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(textureLocation),
                (pose, vertices) -> emitQuad(pose, vertices, request));
    }

    /**
     * GUI state only records textures that are already ready.  A first request
     * schedules upload after extraction, so it is retried on the following
     * frame instead of issuing GPU work while GUI render state is extracted.
     */
    public Identifier getReadyGuiTexture() {
        if (!requiresUpload) return textureLocation;
        if (!guiUploadQueued) {
            guiUploadQueued = true;
            Minecraft.getInstance().execute(() -> {
                try {
                    if (requiresUpload) {
                        updateTexture();
                        requiresUpload = false;
                    }
                } finally {
                    guiUploadQueued = false;
                }
            });
        }
        return null;
    }

    public void close() {
        Minecraft.getInstance().getTextureManager().release(textureLocation);
        if (this.texture != null) this.texture.close();
    }

    private void ensureTexture() {
        int mipmaps = Math.min(Minecraft.getInstance().options.mipmapLevels().get(), maxMipLevel(image.width(), image.height()));
        if (texture != null && texture.width() == image.width() && texture.height() == image.height()) return;
        if (texture != null) texture.close();
        texture = new MipmappedDynamicTexture(textureLocation, image.width(), image.height(), mipmaps);
        Minecraft.getInstance().getTextureManager().register(textureLocation, texture);
    }

    private static int maxMipLevel(int width, int height) {
        int smallest = Math.min(width, height);
        return smallest > 1 ? 31 - Integer.numberOfLeadingZeros(smallest) : 0;
    }

    static void emitQuad(PoseStack.Pose pose, VertexConsumer vertices, ImageRenderRequest request) {
        vertices.addVertex(pose, request.minX(), request.maxY(), 0).setColor(request.red(), request.green(), request.blue(), request.alpha())
                .setUv(request.minU(), request.maxV()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(request.packedLight()).setNormal(pose, 0, 0, 1);
        vertices.addVertex(pose, request.maxX(), request.maxY(), 0).setColor(request.red(), request.green(), request.blue(), request.alpha())
                .setUv(request.maxU(), request.maxV()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(request.packedLight()).setNormal(pose, 0, 0, 1);
        vertices.addVertex(pose, request.maxX(), request.minY(), 0).setColor(request.red(), request.green(), request.blue(), request.alpha())
                .setUv(request.maxU(), request.minV()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(request.packedLight()).setNormal(pose, 0, 0, 1);
        vertices.addVertex(pose, request.minX(), request.minY(), 0).setColor(request.red(), request.green(), request.blue(), request.alpha())
                .setUv(request.minU(), request.minV()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(request.packedLight()).setNormal(pose, 0, 0, 1);
    }
}
