package io.github.mortuusars.exposure.client.render.photograph;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Lightweight first/third-person request. A single photograph reuses the
 * Phase 15 request directly; a stacked item adds at most two paper-only layers.
 */
public record HeldPhotographRenderRequest(
        PhotographRenderer.PhotographRenderRequest photograph,
        List<StackedPaperRenderRequest> stackedPapers,
        boolean stacked) {
    public static final HeldPhotographRenderRequest EMPTY = new HeldPhotographRenderRequest(
            PhotographRenderer.PhotographRenderRequest.EMPTY, List.of(), false);

    public HeldPhotographRenderRequest {
        stackedPapers = List.copyOf(stackedPapers);
    }

    public boolean isEmpty() {
        return photograph.isEmpty();
    }

    public record StackedPaperRenderRequest(Identifier texture, int rotation,
                                            float offset, float zOffset,
                                            int packedLight, int red, int green, int blue, int alpha) {
    }
}
