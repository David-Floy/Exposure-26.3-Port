package io.github.mortuusars.exposure.client.capture.saving;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.client.image.Image;
import io.github.mortuusars.exposure.util.color.Color;
import org.slf4j.Logger;

import java.io.File;

public class NativeImageFileSaver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final File file;

    public NativeImageFileSaver(File file) {
        this.file = file;
    }

    public NativeImageFileSaver(String filePath) {
        this.file = new File(filePath);
    }

    public boolean save(Image image) {
        try (NativeImage nativeImage = new NativeImage(image.width(), image.height(), false)) {
            for (int y = 0; y < image.height(); y++) {
                for (int x = 0; x < image.width(); x++) {
                    int pixelColor = image.getPixelARGB(x, y);
                    nativeImage.setPixelABGR(x, y, Color.ARGBtoABGR(pixelColor));
                }
            }

            boolean ignored = file.getParentFile().mkdirs();
            nativeImage.writeToFile(file);
            LOGGER.info("Saved image: {}", file);
            return true;
        }
        catch (Exception e) {
            LOGGER.error("Failed to save image to file: {}", e.toString());
            return false;
        }
    }
}
