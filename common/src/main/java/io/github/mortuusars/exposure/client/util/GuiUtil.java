package io.github.mortuusars.exposure.client.util;

import io.github.mortuusars.exposure.util.Rect2f;
import net.minecraft.util.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class GuiUtil {

    public static void drawRect(GuiGraphicsExtractor guiGraphics, Rect2f rect, int color) {
        drawRect(guiGraphics, rect.x, rect.y, rect.width, rect.height, color);
    }

    public static void drawRect(GuiGraphicsExtractor guiGraphics, float x, float y, float width, float height, int color) {
        guiGraphics.fill(Mth.floor(x), Mth.floor(y), Mth.floor(x + width), Mth.floor(y + height), color);
    }


    public static void renderScrollingString(GuiGraphicsExtractor guiGraphics, Font font, Component text, int x, int y, int width, int color) {
        renderScrollingString(guiGraphics, font, text, x, y, x + width, y + font.lineHeight, color);
    }

    public static void renderScrollingString(GuiGraphicsExtractor guiGraphics, Font font, Component text, int minX, int minY, int maxX, int maxY, int color) {
        renderScrollingString(guiGraphics, font, text, (minX + maxX) / 2, minX, minY, maxX, maxY, color);
    }

    // Doesn't work in toast for some reason.
    public static void renderScrollingString(GuiGraphicsExtractor guiGraphics, Font font, Component text, int centerX, int minX, int minY, int maxX, int maxY, int color) {
        int fontWidth = font.width(text);
        int y = (minY + maxY - 9) / 2 + 1;
        int width = maxX - minX;
        if (fontWidth > width) {
            int remaining = fontWidth - width;
            double d = (double) Util.getMillis() / 400;
            double e = Math.max((double)remaining * 0.5, 3.0);
            double f = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * d / e)) / 2.0 + 0.5;
            double g = Mth.lerp(f, 0.0, remaining);
            guiGraphics.enableScissor(minX, minY, maxX, maxY);
            guiGraphics.text(font, text, minX - (int)g, y, color, false);
            guiGraphics.disableScissor();
        } else {
            int l = Mth.clamp(centerX, minX + fontWidth / 2, maxX - fontWidth / 2);
            guiGraphics.centeredText(font, text, l, y, color);
        }
    }
}
