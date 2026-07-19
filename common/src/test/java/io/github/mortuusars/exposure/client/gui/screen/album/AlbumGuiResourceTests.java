package io.github.mortuusars.exposure.client.gui.screen.album;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlbumGuiResourceTests {
    @Test
    void signingBackgroundUsesTheOpaqueSigningPanelInsteadOfTheTransparentLegacyCoordinate() throws IOException {
        AlbumGUI.TextureRegion region = AlbumGUI.SIGNING_BACKGROUND;
        assertEquals(298, region.u());
        assertEquals(0, region.v());
        assertEquals(149, region.width());
        assertEquals(188, region.height());

        try (InputStream stream = AlbumGuiResourceTests.class.getResourceAsStream("/assets/exposure/textures/gui/album.png")) {
            assertNotNull(stream, "Missing album GUI atlas");
            BufferedImage atlas = ImageIO.read(stream);
            assertNotNull(atlas, "Cannot decode album GUI atlas");
            assertTrue(nonTransparentPixels(atlas, region.u(), region.v(), region.width(), region.height()) > 0,
                    "The signing screen must sample its opaque panel region");
            assertEquals(0, nonTransparentPixels(atlas, 0, 298, region.width(), region.height()),
                    "The former 0,298 source rectangle is transparent in the 512x512 atlas");
        }
    }

    private static int nonTransparentPixels(BufferedImage image, int x, int y, int width, int height) {
        int count = 0;
        for (int py = y; py < y + height; py++) {
            for (int px = x; px < x + width; px++) {
                if ((image.getRGB(px, py) >>> 24) != 0) count++;
            }
        }
        return count;
    }
}
