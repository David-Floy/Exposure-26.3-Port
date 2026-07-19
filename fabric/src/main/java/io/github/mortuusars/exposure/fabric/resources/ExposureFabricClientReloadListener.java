package io.github.mortuusars.exposure.fabric.resources;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.client.ExposureClientReloadListener;
import net.minecraft.resources.Identifier;

public class ExposureFabricClientReloadListener extends ExposureClientReloadListener {
    public static final Identifier ID = Exposure.resource("clear_client_exposures_cache");
}
