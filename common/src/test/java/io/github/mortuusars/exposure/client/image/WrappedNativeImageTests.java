package io.github.mortuusars.exposure.client.image;

import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WrappedNativeImageTests {
    @Test
    void nativeImagePixelsEnterExposureAsArgbWithoutASecondChannelSwap() {
        int[] colors = {
                0xFFFF0000, // red
                0xFF00FF00, // green
                0xFF0000FF, // blue
                0xFF00FFFF, // cyan
                0xFFFF00FF, // magenta
                0xFFFFFF00, // yellow
                0xFFFFFFFF, // white
                0xFF808080, // 50% gray
                0xFF000000, // black
                0x80402010  // partial alpha with distinct channels
        };

        NativeImage nativeImage = new NativeImage(colors.length, 1, false);
        for (int x = 0; x < colors.length; x++) {
            nativeImage.setPixel(x, 0, colors[x]);
        }

        try (WrappedNativeImage image = new WrappedNativeImage(nativeImage)) {
            for (int x = 0; x < colors.length; x++) {
                assertEquals(colors[x], image.getPixelARGB(x, 0),
                        "ARGB channel layout changed at sample " + x);
            }
        }
    }
}
