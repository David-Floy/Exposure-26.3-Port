package io.github.mortuusars.exposure.world.level.storage;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExposureRepositoryPathTests {
    @Test
    void manualExposureFileOperationsUseTheOverworldSavedDataFolder() {
        Path worldRoot = Path.of("world-root");

        Path overworldFolder = ExposureRepository.getOverworldFolderPath(worldRoot);

        assertEquals(worldRoot.resolve("dimensions/minecraft/overworld"), overworldFolder);
        assertEquals(worldRoot.resolve("dimensions/minecraft/overworld/data/exposures"),
                overworldFolder.resolve("data/" + ExposureRepository.EXPOSURES_DIRECTORY_NAME));
    }
}
