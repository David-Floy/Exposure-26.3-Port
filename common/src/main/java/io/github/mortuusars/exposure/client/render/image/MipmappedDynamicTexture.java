package io.github.mortuusars.exposure.client.render.image;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import io.github.mortuusars.exposure.Exposure;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;

/**
 * A dynamic image texture with the same nearest+mipmap sampling contract used
 * by Exposure before 26.1.2.  Vanilla's DynamicTexture is deliberately a
 * single-level, nearest-repeat texture in this version, so it cannot represent
 * an already mipmapped photograph by itself.
 */
final class MipmappedDynamicTexture extends AbstractTexture {
    private final String label;
    private final Identifier id;
    private NativeImage pixels;
    private int mipLevels;

    MipmappedDynamicTexture(Identifier id, int width, int height, int mipLevels) {
        this.label = "Exposure photograph " + id;
        this.id = id;
        this.pixels = new NativeImage(width, height, false);
        this.mipLevels = mipLevels;
        createTexture();
    }

    int width() {
        return pixels.getWidth();
    }

    int height() {
        return pixels.getHeight();
    }

    void upload() {
        if (texture == null || pixels.isClosed()) return;

        if (mipLevels == 0) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, pixels);
            return;
        }

        // SpriteContents owns the mip images it generates.  Give it a copy so
        // the cached CPU pixels remain valid for a later invalidation/upload.
        NativeImage uploadImage = copyPixels();
        try (SpriteContents contents = new SpriteContents(id, new FrameSize(width(), height()), uploadImage)) {
            contents.increaseMipLevel(mipLevels);
            for (int level = 0; level <= mipLevels; level++) {
                contents.uploadFirstFrame(texture, level);
            }
        } catch (Exception exception) {
            Exposure.LOGGER.error("Failed to generate photograph mipmaps for {}", id, exception);
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, pixels);
        }
    }

    NativeImage pixels() {
        return pixels;
    }

    @Override
    public void close() {
        pixels.close();
        super.close();
    }

    private void createTexture() {
        int usage = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING;
        texture = RenderSystem.getDevice().createTexture(label, usage, TextureFormat.RGBA8,
                width(), height(), 1, mipLevels + 1);
        textureView = RenderSystem.getDevice().createTextureView(texture);
        // The old TextureStateShard used blur=false and mipmap=true.  Its
        // wrapping was the DynamicTexture default (repeat), retained here.
        sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST, mipLevels > 0);
    }

    private NativeImage copyPixels() {
        NativeImage copy = new NativeImage(width(), height(), false);
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                copy.setPixel(x, y, pixels.getPixel(x, y));
            }
        }
        return copy;
    }
}
