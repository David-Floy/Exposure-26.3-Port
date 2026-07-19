package io.github.mortuusars.exposure.client.capture;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.client.util.Shader;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Capture-scoped post-effect selection. Compiled GPU resources are owned by ShaderManager; this
 * class only retains the selected resource identifier between a capture action's before/after
 * callbacks.
 */
public final class CaptureShader {
    private static @Nullable Identifier shaderLocation;

    private CaptureShader() {
    }

    public static boolean hasShader() {
        return shaderLocation != null;
    }

    public static void apply(Identifier location) {
        RenderSystem.assertOnRenderThread();
        if (location.equals(shaderLocation)) {
            return;
        }

        shaderLocation = null;
        if (Shader.isAvailable(location)) {
            shaderLocation = location;
        } else {
            Exposure.LOGGER.warn("Failed to select capture post effect '{}'.", location);
        }
    }

    public static void process() {
        Identifier location = shaderLocation;
        if (location != null) {
            Shader.process(location, Minecrft.get().getMainRenderTarget());
        }
    }

    public static void process(RenderTarget renderTarget) {
        Identifier location = shaderLocation;
        if (location != null) {
            Shader.process(location, renderTarget);
        }
    }

    public static void remove() {
        RenderSystem.assertOnRenderThread();
        shaderLocation = null;
    }
}
