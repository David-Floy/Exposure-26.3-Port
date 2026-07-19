package io.github.mortuusars.exposure.mixin.client;

import io.github.mortuusars.exposure.client.render.GammaModifier;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class ExposureLightTextureMixin {
    @Inject(method = "extract", at = @At("RETURN"))
    private void exposure$modifyBrightness(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        if (state.needsUpdate) {
            state.brightness = GammaModifier.getModifiedValue(state.brightness);
        }
    }
}
