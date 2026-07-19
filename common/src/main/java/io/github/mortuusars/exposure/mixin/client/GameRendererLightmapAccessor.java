package io.github.mortuusars.exposure.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Lightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererLightmapAccessor {
    @Accessor("lightmap")
    Lightmap exposure$getLightmap();
}
