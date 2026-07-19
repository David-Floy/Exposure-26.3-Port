package io.github.mortuusars.exposure.client.render;

import io.github.mortuusars.exposure.mixin.client.GameRendererLightmapAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.Mth;

public class GammaModifier {
    private static float offset = 0f;

    public static float getOffset() {
        return offset;
    }

    public static void apply(float offsetValue) {
        offsetValue = Mth.clamp(offsetValue, -1F, 1F);
        if (offset != offsetValue) {
            float previousOffset = offset;
            offset = offsetValue;
            updateCurrentLightmap(previousOffset, offset);
        }
    }

    public static void restore() {
        if (offset != 0f) {
            float previousOffset = offset;
            offset = 0f;
            updateCurrentLightmap(previousOffset, offset);
        }
    }

    public static float getModifiedValue(float original) {
        return original + offset;
    }

    private static void updateCurrentLightmap(float previousOffset, float newOffset) {
        Minecraft minecraft = Minecraft.getInstance();
        LightmapRenderState state = minecraft.gameRenderer.getGameRenderState().lightmapRenderState;
        state.brightness += newOffset - previousOffset;
        state.needsUpdate = true;
        ((GameRendererLightmapAccessor) minecraft.gameRenderer).exposure$getLightmap().render(state);
    }
}
