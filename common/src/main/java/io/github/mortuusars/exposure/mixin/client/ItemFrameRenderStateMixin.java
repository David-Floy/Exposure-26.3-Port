package io.github.mortuusars.exposure.mixin.client;

import io.github.mortuusars.exposure.client.render.photograph.ItemFramePhotoRenderState;
import io.github.mortuusars.exposure.client.render.photograph.PhotographRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemFrameRenderState.class)
public class ItemFrameRenderStateMixin implements ItemFramePhotoRenderState {
    @Unique private PhotographRenderer.PhotographRenderRequest exposure$photographRequest = PhotographRenderer.PhotographRenderRequest.EMPTY;

    @Override public PhotographRenderer.PhotographRenderRequest exposure$getPhotographRequest() { return exposure$photographRequest; }
    @Override public void exposure$setPhotographRequest(PhotographRenderer.PhotographRenderRequest request) { exposure$photographRequest = request; }
}
