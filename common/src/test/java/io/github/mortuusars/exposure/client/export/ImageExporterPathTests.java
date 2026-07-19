package io.github.mortuusars.exposure.client.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageExporterPathTests {
    @Test
    void preservesSafeIdSubfoldersInsideTheExportFolder() {
        Path folder = Path.of("exports").toAbsolutePath();

        Path output = ImageExporter.resolveOutputPath(folder, "Test World", "album/photo_01");

        assertEquals(folder.resolve("Test World/album/photo_01.png"), output);
        assertTrue(output.startsWith(folder));
    }

    @Test
    void sanitizesTraversalAndWindowsIllegalNames() {
        Path folder = Path.of("exports").toAbsolutePath();

        Path output = ImageExporter.resolveOutputPath(folder, "../World", "../CON:shot?*");

        assertEquals(folder.resolve("_World/_/CON_shot__.png"), output);
        assertTrue(output.startsWith(folder));
    }
}
