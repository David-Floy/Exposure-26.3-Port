package io.github.mortuusars.exposure.client.render.image;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.exposure.client.image.renderable.RenderableImage;
import io.github.mortuusars.exposure.client.image.renderable.RenderableImageIdentifier;
import io.github.mortuusars.exposure.util.color.Color;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ImageRenderer implements AutoCloseable {
    private final Map<RenderableImageIdentifier, RenderedImageInstance> cache = new HashMap<>();

    public ImageRenderRequest request(RenderableImage image, RenderCoordinates coords,
                                      int packedLight, Color color) {
        return request(image, coords, packedLight, color.getR(), color.getG(), color.getB(), color.getA());
    }

    public ImageRenderRequest request(RenderableImage image, RenderCoordinates coords,
                                      int packedLight, int r, int g, int b, int a) {
        return request(image, coords.minX(), coords.minY(), coords.maxX(), coords.maxY(),
                coords.minU(), coords.minV(), coords.maxU(), coords.maxV(), packedLight, r, g, b, a);
    }

    public ImageRenderRequest request(RenderableImage image,
                       float minX, float minY, float maxX, float maxY,
                       float minU, float minV, float maxU, float maxV, int packedLight, int r, int g, int b, int a) {
        getOrCreateInstance(image);
        return new ImageRenderRequest(image.getIdentifier(), minX, minY, maxX, maxY, minU, minV, maxU, maxV, packedLight, r, g, b, a);
    }

    public void submit(ImageRenderRequest request, PoseStack poseStack, SubmitNodeCollector collector) {
        RenderedImageInstance instance = cache.get(request.imageId());
        if (instance != null) instance.submit(poseStack, collector, request);
    }

    /** Submits a normal resource texture using the exact same quad/light/color contract as a photograph. */
    public void submitTexture(Identifier texture, ImageRenderRequest request, PoseStack poseStack, SubmitNodeCollector collector) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture),
                (pose, vertices) -> RenderedImageInstance.emitQuad(pose, vertices, request));
    }

    /** GUI records an Identifier into GuiRenderState; the dynamic texture is made ready before that reference is recorded. */
    public boolean renderGui(RenderableImage image, GuiGraphicsExtractor guiGraphics, float x, float y, float width, float height, Color color) {
        if (image.isEmpty()) return false;
        Identifier texture = getOrCreateInstance(image).getReadyGuiTexture();
        if (texture == null) return false;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, Math.round(x), Math.round(y), 0, 0,
                Math.round(width), Math.round(height), Math.round(width), Math.round(height), color.getARGB());
        return true;
    }

    private RenderedImageInstance getOrCreateInstance(RenderableImage image) {
        return (this.cache).compute(image.getIdentifier(), (id, expData) -> {
            if (expData == null) {
                return new RenderedImageInstance(image);
            }
            expData.replaceData(image);
            return expData;
        });
    }

    public void clearCache() {
        cache.values().forEach(RenderedImageInstance::close);
        cache.clear();
    }

    public void clearCacheOf(String baseID) {
        cache.entrySet().removeIf(entry -> {
            boolean shouldRemove = entry.getKey().base().equals(baseID);
            if (shouldRemove) {
                entry.getValue().close();
            }
            return shouldRemove;
        });
    }

    public void clearCacheOf(Predicate<RenderableImageIdentifier> predicate) {
        cache.entrySet().removeIf(entry -> {
            boolean shouldRemove = predicate.test(entry.getKey());
            if (shouldRemove) {
                entry.getValue().close();
            }
            return shouldRemove;
        });
    }

    @Override
    public void close() {
        clearCache();
    }
}
