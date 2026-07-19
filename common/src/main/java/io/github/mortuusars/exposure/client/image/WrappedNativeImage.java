package io.github.mortuusars.exposure.client.image;

import com.mojang.blaze3d.platform.NativeImage;

public class WrappedNativeImage implements Image {
    private final NativeImage nativeImage;
    private boolean closed;

    public WrappedNativeImage(NativeImage nativeImage) {
        this.nativeImage = nativeImage;
    }

    @Override
    public int width() {
        return nativeImage.getWidth();
    }

    @Override
    public int height() {
        return nativeImage.getHeight();
    }

    @Override
    public int getPixelARGB(int x, int y) {
        // Since 26.1.2 NativeImage#getPixel converts its internal ABGR storage to ARGB.
        // The fixed 1.21.1 upstream conversion must therefore not be applied a second time.
        return nativeImage.getPixel(x, y);
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            nativeImage.close();
        }
    }
}
