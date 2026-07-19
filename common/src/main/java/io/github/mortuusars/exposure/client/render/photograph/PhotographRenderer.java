package io.github.mortuusars.exposure.client.render.photograph;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.image.renderable.RenderableImage;
import io.github.mortuusars.exposure.client.render.image.ImageRenderRequest;
import io.github.mortuusars.exposure.client.render.image.RenderCoordinates;
import io.github.mortuusars.exposure.client.render.texture.TextureRenderer;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.StackedPhotographsItem;
import io.github.mortuusars.exposure.world.item.util.ItemAndStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import io.github.mortuusars.exposure.util.color.Color;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Builds lightweight photo requests during extraction and submits them later without retaining ItemStacks or pixels. */
public class PhotographRenderer {
    public PhotographRenderRequest prepare(ItemStack stack, boolean paper, boolean backside,
                                           int packedLight, int red, int green, int blue, int alpha) {
        if (!(stack.getItem() instanceof PhotographItem item)) return PhotographRenderRequest.EMPTY;
        PhotographStyle style = PhotographStyle.of(stack);
        Frame frame = item.getFrame(stack);
        RenderableImage image = style.process(ExposureClient.renderedExposures().getOrCreate(frame));
        if (image.isEmpty()) return PhotographRenderRequest.EMPTY;
        ImageRenderRequest imageRequest = ExposureClient.imageRenderer().request(image, RenderCoordinates.DEFAULT,
                packedLight, red, green, blue, alpha);
        return new PhotographRenderRequest(imageRequest, style.paperTexture(), style.overlayTexture(),
                paper, backside, frame.identifier().hashCode() % 4 * 90);
    }

    public void submit(PhotographRenderRequest request, PoseStack poseStack, SubmitNodeCollector collector) {
        if (request.isEmpty()) return;
        if (request.renderPaper()) {
            poseStack.pushPose();
            rotatePaper(poseStack, request.paperRotation());
            TextureRenderer.submit(poseStack, collector, request.paperTexture(), request.image().packedLight(),
                    request.image().red(), request.image().green(), request.image().blue(), request.image().alpha());
            poseStack.popPose();
            if (request.renderBackside()) submitBackside(request, poseStack, collector);
        }

        poseStack.pushPose();
        if (request.renderPaper()) {
            poseStack.translate(0.0625f, 0.0625f, 0.001f);
            poseStack.scale(0.875f, 0.875f, 0.875f);
        }
        ExposureClient.imageRenderer().submit(request.image(), poseStack, collector);
        poseStack.popPose();

        if (request.renderPaper() && request.overlayTexture() != ExposureClient.Textures.EMPTY) {
            poseStack.pushPose();
            rotatePaper(poseStack, request.paperRotation());
            poseStack.translate(0, 0, 0.002f);
            TextureRenderer.submit(poseStack, collector, request.overlayTexture(), request.image().packedLight(),
                    request.image().red(), request.image().green(), request.image().blue(), request.image().alpha());
            poseStack.popPose();
        }
    }

    /**
     * Builds the complete legacy held-item representation without retaining
     * the source stack. Stacked photographs keep only the top image request
     * and the two visible paper layers below it.
     */
    public HeldPhotographRenderRequest prepareHeld(ItemStack stack, boolean paper, boolean backside,
                                                   int packedLight, int red, int green, int blue, int alpha) {
        if (stack.getItem() instanceof PhotographItem) {
            PhotographRenderRequest request = prepare(stack, paper, backside, packedLight, red, green, blue, alpha);
            return request.isEmpty()
                    ? HeldPhotographRenderRequest.EMPTY
                    : new HeldPhotographRenderRequest(request, List.of(), false);
        }
        if (!(stack.getItem() instanceof StackedPhotographsItem stackedItem)) {
            return HeldPhotographRenderRequest.EMPTY;
        }

        List<ItemAndStack<PhotographItem>> photographs = stackedItem.getPhotographs(stack).photographsItemAndStacks();
        if (photographs.isEmpty()) return HeldPhotographRenderRequest.EMPTY;

        PhotographRenderRequest top = prepare(photographs.getFirst().getItemStack(), true, false,
                packedLight, red, green, blue, alpha);
        if (top.isEmpty()) return HeldPhotographRenderRequest.EMPTY;

        List<HeldPhotographRenderRequest.StackedPaperRenderRequest> papers = new ArrayList<>(2);
        for (int i = Math.min(2, photographs.size() - 1); i >= 1; i--) {
            ItemAndStack<PhotographItem> photograph = photographs.get(i);
            ItemStack photographStack = photograph.getItemStack();
            Frame frame = photograph.getItem().getFrame(photographStack);
            PhotographStyle style = PhotographStyle.of(photographStack);
            float brightness = 1f - getStackedBrightnessStep() * i;
            papers.add(new HeldPhotographRenderRequest.StackedPaperRenderRequest(
                    style.paperTexture(), frame.identifier().hashCode() % 4 * 90,
                    getStackedPhotographOffset() * i, 0.002f - i / 1000f,
                    packedLight, (int) (red * brightness), (int) (green * brightness),
                    (int) (blue * brightness), alpha));
        }
        return new HeldPhotographRenderRequest(top, papers, true);
    }

    /** Submits a held single/stacked request in the same back-to-front order as the legacy renderer. */
    public void submitHeld(HeldPhotographRenderRequest request, PoseStack poseStack, SubmitNodeCollector collector) {
        if (request.isEmpty()) return;
        for (HeldPhotographRenderRequest.StackedPaperRenderRequest paper : request.stackedPapers()) {
            poseStack.pushPose();
            poseStack.translate(paper.offset(), paper.offset(), paper.zOffset());
            rotatePaper(poseStack, paper.rotation());
            TextureRenderer.submit(poseStack, collector, paper.texture(), paper.packedLight(),
                    paper.red(), paper.green(), paper.blue(), paper.alpha());
            poseStack.popPose();
        }

        poseStack.pushPose();
        if (request.stacked()) poseStack.translate(0, 0, 0.002f);
        submit(request.photograph(), poseStack, collector);
        poseStack.popPose();
    }

    public boolean renderGui(ItemStack stack, GuiGraphicsExtractor guiGraphics, float x, float y, float size, Color color, boolean paper) {
        if (!(stack.getItem() instanceof PhotographItem item)) return false;
        PhotographStyle style = PhotographStyle.of(stack);
        if (paper && style.paperTexture() != ExposureClient.Textures.EMPTY) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, style.paperTexture(), Math.round(x), Math.round(y), 0, 0,
                    Math.round(size), Math.round(size), Math.round(size), Math.round(size), color.getARGB());
        }
        RenderableImage image = style.process(ExposureClient.renderedExposures().getOrCreate(item.getFrame(stack)));
        float inset = paper ? size * 0.0625f : 0;
        float imageSize = paper ? size * 0.875f : size;
        boolean rendered = ExposureClient.imageRenderer().renderGui(image, guiGraphics, x + inset, y + inset, imageSize, imageSize, color);
        if (paper && style.hasOverlayTexture()) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, style.overlayTexture(), Math.round(x), Math.round(y), 0, 0,
                    Math.round(size), Math.round(size), Math.round(size), Math.round(size), color.getARGB());
        }
        return rendered;
    }

    private void submitBackside(PhotographRenderRequest request, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.translate(-0.5, 0, -0.5);
        rotatePaper(poseStack, request.paperRotation());
        TextureRenderer.submit(poseStack, collector, request.paperTexture(), request.image().packedLight(),
                (int) (request.image().red() * 0.85f), (int) (request.image().green() * 0.85f),
                (int) (request.image().blue() * 0.85f), request.image().alpha());
        poseStack.popPose();
    }

    private static void rotatePaper(PoseStack poseStack, int rotation) {
        poseStack.translate(0.5f, 0.5f, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        poseStack.translate(-0.5f, -0.5f, 0);
    }

    public float getStackedBrightnessStep() { return 0.2f; }
    public float getStackedPhotographOffset() { return 0.03125f; }

    public record PhotographRenderRequest(ImageRenderRequest image, Identifier paperTexture, Identifier overlayTexture,
                                          boolean renderPaper, boolean renderBackside, int paperRotation) {
        public static final PhotographRenderRequest EMPTY = new PhotographRenderRequest(null, null, null, false, false, 0);
        public boolean isEmpty() { return image == null; }
    }
}
