package io.github.mortuusars.exposure;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;

public class PlatformHelperClient {
    private static Service service;

    public static void bind(Service implementation) {
        if (service != null) {
            throw new IllegalStateException("Exposure client platform service is already bound.");
        }
        service = java.util.Objects.requireNonNull(implementation, "implementation");
    }

    public static BlockStateModel getModel(Identifier model) {
        if (service == null) {
            throw new IllegalStateException("Exposure client platform service has not been bound by the Fabric client entrypoint.");
        }
        return service.getModel(model);
    }

    public interface Service {
        BlockStateModel getModel(Identifier model);
    }
}
