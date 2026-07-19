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
        this.currentEffect = Minecrft.get().gameRenderer.currentPostEffect();
        this.effectActive = Minecrft.get().gameRenderer.effectActive;

        Shader.applyToGameRenderer(effect);
    }

    @Override
    public void afterCapture() {
        Minecrft.get().gameRenderer.effectActive = effectActive;
        if (currentEffect != null) {
            Shader.applyToGameRenderer(currentEffect);
            Minecrft.get().gameRenderer.effectActive = effectActive;
        } else {
            Shader.clearGameRendererEffect();
        }
    }
}
