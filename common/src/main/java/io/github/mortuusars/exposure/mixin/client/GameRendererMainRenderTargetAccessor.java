package io.github.mortuusars.exposure.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the main render target field directly, bypassing the background capture redirect that is applied to
 * {@code GameRenderer#mainRenderTarget()}.
 */
@Mixin(GameRenderer.class)
public interface GameRendererMainRenderTargetAccessor {
    @Accessor("mainRenderTarget")
    RenderTarget exposure$getMainRenderTarget();
}
