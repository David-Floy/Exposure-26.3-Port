package io.github.mortuusars.exposure.client.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.client.camera.CameraClient;
import io.github.mortuusars.exposure.client.capture.CaptureShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entry point for post effects used by Exposure.
 *
 * <p>Compiled chains are owned by Minecraft's {@code ShaderManager}. Exposure only keeps the
 * requested resource identifier, so resource reload can replace and close the manager's cache
 * without leaving stale GPU objects in mod-owned state.</p>
 */
public final class Shader {
    private static final String LEGACY_POST_EFFECT_PREFIX = "shaders/post/";
    private static final String LEGACY_POST_EFFECT_SUFFIX = ".json";

    private static boolean suppressViewfinder;

    private Shader() {
    }

    /**
     * Maps Exposure's stable, legacy post-effect resource identifiers to the identifiers used by
     * the 26.1.2 ShaderManager. The value is only used as a resource-manager key; it is never
     * resolved as a filesystem path.
     */
    public static Identifier postEffectId(@NotNull Identifier shaderLocation) {
        String path = shaderLocation.getPath();
        if (path.startsWith(LEGACY_POST_EFFECT_PREFIX)
                && path.endsWith(LEGACY_POST_EFFECT_SUFFIX)
                && path.length() > LEGACY_POST_EFFECT_PREFIX.length() + LEGACY_POST_EFFECT_SUFFIX.length()) {
            String postEffectPath = path.substring(LEGACY_POST_EFFECT_PREFIX.length(),
                    path.length() - LEGACY_POST_EFFECT_SUFFIX.length());
            return Identifier.fromNamespaceAndPath(shaderLocation.getNamespace(), postEffectPath);
        }
        return shaderLocation;
    }

    public static boolean isAvailable(@NotNull Identifier shaderLocation) {
        RenderSystem.assertOnRenderThread();
        return getPostChain(shaderLocation) != null;
    }

    /**
     * Processes the requested post effect into the supplied target. Internal transient targets
     * are framegraph resources; persistent targets and compiled programs remain owned by the
     * ShaderManager and are released on resource reload/client shutdown.
     */
    public static boolean process(@NotNull Identifier shaderLocation, @NotNull RenderTarget renderTarget) {
        RenderSystem.assertOnRenderThread();
        PostChain postChain = getPostChain(shaderLocation);
        if (postChain == null) {
            return false;
        }

        postChain.process(renderTarget, GraphicsResourceAllocator.UNPOOLED);
        return true;
    }

    /**
     * Validates and applies a post effect to the vanilla GameRenderer. A failed lookup clears the
     * requested effect, leaving subsequent vanilla rendering in a known state.
     */
    public static boolean applyToGameRenderer(@NotNull Identifier shaderLocation) {
        RenderSystem.assertOnRenderThread();
        Identifier postEffectId = postEffectId(shaderLocation);
        Minecraft minecraft = Minecrft.get();
        if (minecraft.getShaderManager().getPostChain(postEffectId, LevelTargetBundle.MAIN_TARGETS) == null) {
            Exposure.LOGGER.warn("Cannot apply unavailable post effect '{}' (resolved as '{}').",
                    shaderLocation, postEffectId);
            minecraft.gameRenderer.clearPostEffect();
            return false;
        }

        minecraft.gameRenderer.setPostEffect(postEffectId);
        return true;
    }

    public static void clearGameRendererEffect() {
        RenderSystem.assertOnRenderThread();
        Minecrft.get().gameRenderer.clearPostEffect();
    }

    private static @Nullable PostChain getPostChain(Identifier shaderLocation) {
        Minecraft minecraft = Minecrft.get();
        return minecraft.getShaderManager().getPostChain(postEffectId(shaderLocation), LevelTargetBundle.MAIN_TARGETS);
    }

    public static void setSuppressViewfinder(boolean suppress) {
        suppressViewfinder = suppress;
    }

    public static void processForGameRenderer() {
        RenderSystem.assertOnRenderThread();
        if (!suppressViewfinder && CameraClient.viewfinder() != null) {
            CameraClient.viewfinder().shader().process();
        }

        if (CaptureShader.hasShader()) {
            CaptureShader.process();
        }
    }
}
