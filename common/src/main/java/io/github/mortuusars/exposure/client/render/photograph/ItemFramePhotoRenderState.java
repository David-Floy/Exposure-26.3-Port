package io.github.mortuusars.exposure.client.render.photograph;

/** Mixin bridge: ItemFrameRenderState stores a request, never the source ItemStack or pixels. */
public interface ItemFramePhotoRenderState {
    PhotographRenderer.PhotographRenderRequest exposure$getPhotographRequest();
    void exposure$setPhotographRequest(PhotographRenderer.PhotographRenderRequest request);
}
