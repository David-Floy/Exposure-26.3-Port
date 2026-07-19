package io.github.mortuusars.exposure.client.export;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.client.image.Image;
import io.github.mortuusars.exposure.client.image.PalettedImage;
import io.github.mortuusars.exposure.client.util.LevelNameGetter;
import io.github.mortuusars.exposure.util.color.Color;
import io.github.mortuusars.exposure.world.level.storage.ExposureData;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class ImageExporter {
    private static final Set<String> WINDOWS_RESERVED_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");
    protected final Image image;
    protected final String fileName;
    protected String folder = "exposures";
    @Nullable
    protected String worldName = null;
    protected long creationUnixTimestamp = 0;
    protected Function<Image, Image> imageModifier = Function.identity();
    protected Consumer<File> onExport = f -> {};

    public ImageExporter(Image image, String fileName) {
        this.image = image;
        this.fileName = fileName;
    }

    public ImageExporter(ExposureData exposure, String fileName) {
        this(PalettedImage.fromExposure(exposure), fileName);
    }

    public Function<Image, Image> getModifier() { return imageModifier; }
    public String getFileName() { return fileName; }
    public String getFolder() { return folder; }
    public @Nullable String getWorldSubfolder() { return worldName; }
    public long getCreationUnixTimestamp() { return creationUnixTimestamp; }

    public ImageExporter modify(Function<Image, Image> imageModifier) {
        this.imageModifier = imageModifier;
        return this;
    }

    public ImageExporter withFolder(String folder) {
        this.folder = folder;
        return this;
    }

    public ImageExporter toExposuresFolder() {
        this.folder = "exposures";
        return this;
    }

    public ImageExporter organizeByWorld(@Nullable String worldName) {
        this.worldName = worldName;
        return this;
    }

    public ImageExporter organizeByWorld(boolean organize) {
        this.worldName = organize ? LevelNameGetter.getWorldName() : null;
        return this;
    }

    public ImageExporter setCreationDate(long unixTimestamp) {
        this.creationUnixTimestamp = unixTimestamp;
        return this;
    }

    public boolean export() {
        return save(imageModifier.apply(image));
    }

    protected boolean save(Image image) {
        try (NativeImage nativeImage = convertToNativeImage(image)) {
            Path outputPath = resolveOutputPath(Paths.get(getFolder()), getWorldSubfolder(), getFileName());
            Files.createDirectories(outputPath.getParent());
            File outputFile = outputPath.toFile();

            nativeImage.writeToFile(outputFile);

            if (creationUnixTimestamp > 0) {
                trySetFileCreationDate(outputFile.getAbsolutePath(), creationUnixTimestamp);
            }

            onExport.accept(outputFile);

            Exposure.LOGGER.info("Exposure saved: {}", outputFile);
            return true;
        }
        catch (Exception e) {
            Exposure.LOGGER.error("Failed to save exposure to file: {}", e.toString());
            return false;
        }
    }

    static Path resolveOutputPath(Path folder, @Nullable String worldName, String fileName) {
        Path baseFolder = folder.toAbsolutePath().normalize();
        Path outputFolder = worldName == null
                ? baseFolder
                : baseFolder.resolve(sanitizePathComponent(worldName)).normalize();

        Path relativeFile = Paths.get("");
        for (String component : fileName.replace('\\', '/').split("/")) {
            if (!component.isEmpty()) {
                relativeFile = relativeFile.resolve(sanitizePathComponent(component));
            }
        }
        if (relativeFile.getNameCount() == 0) {
            relativeFile = Path.of("exposure");
        }

        Path outputPath = outputFolder.resolve(relativeFile + ".png").normalize();
        if (!outputPath.startsWith(outputFolder)) {
            throw new IllegalArgumentException("Export filename resolves outside the export folder: " + fileName);
        }
        return outputPath;
    }

    static String sanitizePathComponent(String component) {
        StringBuilder sanitized = new StringBuilder(component.length());
        for (int i = 0; i < component.length(); i++) {
            char character = component.charAt(i);
            sanitized.append(character < 32 || "<>:\"/\\|?*".indexOf(character) >= 0 ? '_' : character);
        }

        while (!sanitized.isEmpty() && (sanitized.charAt(sanitized.length() - 1) == '.'
                || sanitized.charAt(sanitized.length() - 1) == ' ')) {
            sanitized.deleteCharAt(sanitized.length() - 1);
        }

        String result = sanitized.toString();
        while (result.startsWith(".")) {
            result = result.substring(1);
        }
        if (result.isEmpty() || result.equals(".") || result.equals("..")) {
            result = "_";
        }
        String baseName = result.split("\\.", 2)[0].toUpperCase(Locale.ROOT);
        return WINDOWS_RESERVED_NAMES.contains(baseName) ? "_" + result : result;
    }

    protected NativeImage convertToNativeImage(Image image) {
        NativeImage nativeImage = new NativeImage(image.width(), image.height(), false);

        for (int x = 0; x < nativeImage.getWidth(); x++) {
            for (int y = 0; y < nativeImage.getHeight(); y++) {
                nativeImage.setPixelABGR(x, y, Color.ARGBtoABGR(image.getPixelARGB(x, y)));
            }
        }

        return nativeImage;
    }

    protected void trySetFileCreationDate(String filePath, long creationTimeUnixSeconds) {
        try {
            Date creationDate = Date.from(Instant.ofEpochSecond(creationTimeUnixSeconds));

            BasicFileAttributeView attributes = Files.getFileAttributeView(Paths.get(filePath), BasicFileAttributeView.class);
            FileTime creationTime = FileTime.fromMillis(creationDate.getTime());
            FileTime modifyTime = FileTime.fromMillis(System.currentTimeMillis());
            attributes.setTimes(modifyTime, modifyTime, creationTime);
        }
        catch (Exception ignored) { }
    }

    public ImageExporter onExport(Consumer<File> onExport) {
        this.onExport = onExport;
        return this;
    }
}
