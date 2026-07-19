package io.github.mortuusars.exposure.client.gui.screen.album;

import io.github.mortuusars.exposure.Exposure;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.Identifier;

public class AlbumGUI {
    public static final WidgetSprites PREVIOUS_PAGE_BUTTON_SPRITES = new WidgetSprites(
            Exposure.resource("album/previous_page"), Exposure.resource("album/previous_page_highlighted"));
    public static final WidgetSprites NEXT_PAGE_BUTTON_SPRITES = new WidgetSprites(
            Exposure.resource("album/next_page"), Exposure.resource("album/next_page_highlighted"));

    public static final Identifier TEXTURE = Exposure.resource("textures/gui/album.png");

    public static final TextureRegion ADDING_INVENTORY_BACKGROUND = new TextureRegion(0, 188, 176, 100);
    public static final TextureRegion DISABLED_INVENTORY_SLOT = new TextureRegion(176, 188, 18, 18);
    public static final TextureRegion SIGNING_BACKGROUND = new TextureRegion(298, 0, 149, 188);

    /** A source region in the 512x512 album texture, rendered at its native GUI size. */
    public record TextureRegion(int u, int v, int width, int height) {
        public void blit(GuiGraphicsExtractor guiGraphics, int x, int y) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, v,
                    width, height, width, height, 512, 512);
        }
    }
}
