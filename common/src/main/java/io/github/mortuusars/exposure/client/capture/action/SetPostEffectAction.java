package io.github.mortuusars.exposure.client.capture.action;

import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.client.util.Shader;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class SetPostEffectAction implements CaptureAction {
    @Nullable
    private Identifier currentEffect;
    private boolean effectActive;

    private final Identifier effect;

    public SetPostEffectAction(Identifier effect) {
        this.effect = effect;
    }

    @Override
    public void beforeCapture() {
        this.currentEffect = Minecrft.get().gameRenderer.spectatedEntityPostEffect();
        this.effectActive = Minecrft.get().gameRenderer.spectatedEntityEffectActive;

        Shader.applyToGameRenderer(effect);
    }

    @Override
    public void afterCapture() {
        Minecrft.get().gameRenderer.spectatedEntityEffectActive = effectActive;
        if (currentEffect != null) {
            Shader.applyToGameRenderer(currentEffect);
            Minecrft.get().gameRenderer.spectatedEntityEffectActive = effectActive;
        } else {
            Shader.clearGameRendererEffect();
        }
    }
}
