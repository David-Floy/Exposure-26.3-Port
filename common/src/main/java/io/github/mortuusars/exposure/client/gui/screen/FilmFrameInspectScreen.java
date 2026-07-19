package io.github.mortuusars.exposure.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.gui.Widgets;
import io.github.mortuusars.exposure.client.gui.component.SteppedZoom;
import io.github.mortuusars.exposure.client.gui.screen.element.Pager;
import io.github.mortuusars.exposure.client.image.modifier.ImageEffect;
import io.github.mortuusars.exposure.client.image.renderable.RenderableImage;
import io.github.mortuusars.exposure.client.input.Key;
import io.github.mortuusars.exposure.client.input.KeyBindings;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.util.PagingDirection;
import io.github.mortuusars.exposure.world.camera.ExposureType;
import io.github.mortuusars.exposure.world.camera.FilmColor;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.sound.SoundEffect;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilmFrameInspectScreen extends Screen {
    public static final Identifier TEXTURE = Exposure.resource("textures/gui/film_frame_inspect.png");

    public static final int BG_SIZE = 78;
    public static final int FRAME_SIZE = 54;

    protected final Pager pager = new Pager()
            .setChangeSound(new SoundEffect(Exposure.SoundEvents.CAMERA_LENS_RING_CLICK))
            .onPageChanged(this::pageChanged);

    protected final SteppedZoom zoom = new SteppedZoom()
            .zoomInSteps(4)
            .zoomOutSteps(4)
            .zoomPerStep(1.4)
            .defaultZoom(1);

    protected final KeyBindings keyBindings = KeyBindings.of(
            Key.press(Minecrft.options().keyInventory).executes(this::onClose),
            Key.press(InputConstants.KEY_ADD).or(Key.press(InputConstants.KEY_EQUALS)).executes(zoom::zoomIn),
            Key.press(GLFW.GLFW_KEY_KP_SUBTRACT).or(Key.press(InputConstants.KEY_MINUS)).executes(zoom::zoomOut),
            Key.press(InputConstants.KEY_LEFT).or(Key.press(InputConstants.KEY_A)).executes(pager::previousPage),
            Key.press(InputConstants.KEY_RIGHT).or(Key.press(InputConstants.KEY_D)).executes(pager::nextPage),
            Key.release(InputConstants.KEY_LEFT).or(Key.press(InputConstants.KEY_A)).executes(pager::resetCooldown),
            Key.release(InputConstants.KEY_RIGHT).or(Key.press(InputConstants.KEY_D)).executes(pager::resetCooldown)
    );

    protected final List<Frame> frames;

    protected float zoomFactor;
    protected float x;
    protected float y;

    public FilmFrameInspectScreen(List<Frame> frames) {
        this(frames, frames.size() - 1);
    }

    public FilmFrameInspectScreen(List<Frame> frames, int startingFrame) {
        super(Component.empty());
        this.frames = new ArrayList<>(frames);
        initPager(startingFrame);
    }

    protected void initPager(int startingFrame) {
        pager.setPagesCount(frames.size());
        pager.setPage(startingFrame);
        Collections.rotate(frames, -startingFrame);
    }

    @Override
    protected void init() {
        super.init();

        zoomFactor = ((float) height / BG_SIZE) / (float)zoom.getZoomPerStep();

        ImageButton previousButton = new ImageButton(0, (int) (height / 2f - 16 / 2f), 16, 16,
                Widgets.PREVIOUS_BUTTON_SPRITES,
                button -> pager.changePage(PagingDirection.PREVIOUS), Component.translatable("gui.exposure.previous_page"));
        addRenderableWidget(previousButton);

        ImageButton nextButton = new ImageButton(width - 16, (int) (height / 2f - 16 / 2f), 16, 16,
                Widgets.NEXT_BUTTON_SPRITES,
                button -> pager.changePage(PagingDirection.NEXT), Component.translatable("gui.exposure.next_page"));
        addRenderableWidget(nextButton);

        pager.setPagesCount(frames.size())
                .setPreviousPageButton(previousButton)
                .setNextPageButton(nextButton);
    }

    protected Frame getCurrentFrame() {
        return frames.getFirst();
    }

    protected void pageChanged(int oldPage, int newPage) {
        int distance = newPage - oldPage;
        Collections.rotate(frames, -distance);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        float scale = (float) (zoom.get() * zoomFactor);

        extractTransparentBackground(guiGraphics);
        float originX = width / 2f + x - BG_SIZE * scale / 2f;
        float originY = height / 2f + y - BG_SIZE * scale / 2f;
        int scaledSize = Math.round(BG_SIZE * scale);
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, Math.round(originX), Math.round(originY),
                0, 0, scaledSize, scaledSize, BG_SIZE, BG_SIZE, 256, 256);

        Frame frame = getCurrentFrame();
        ExposureType filmType = frame.type();
        FilmColor filmColor = filmType.getFilmColor();

        int filmTint = io.github.mortuusars.exposure.util.color.Color.argbF(filmColor.a(), filmColor.r(), filmColor.g(), filmColor.b()).getARGB();
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, Math.round(originX), Math.round(originY),
                0, BG_SIZE, scaledSize, scaledSize, BG_SIZE, BG_SIZE, 256, 256, filmTint);
        RenderableImage image = ExposureClient.renderedExposures().getOrCreate(frame).modifyWith(ImageEffect.NEGATIVE_FILM);
        ExposureClient.imageRenderer().renderGui(image, guiGraphics, originX + 12 * scale, originY + 12 * scale,
                FRAME_SIZE * scale, FRAME_SIZE * scale, filmType.getImageColor());

        // Places widgets above, because they will be covered when photo is zoomed in
        guiGraphics.nextStratum();
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Background is rendered manually in #render method.
        // Otherwise, background will be rendered on top
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyBindings.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return keyBindings.keyReleased(event) || super.keyReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;

        if (scrollY >= 0.0) {
            zoom.zoomIn();
        } else {
            zoom.zoomOut();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (super.mouseDragged(event, dragX, dragY)) return true;

        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            float centerX = width / 2f;
            float centerY = height / 2f;
            x = (float) Mth.clamp(x + dragX, -centerX, centerX);
            y = (float) Mth.clamp(y + dragY, -centerY, centerY);
            return true;
        }

        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
