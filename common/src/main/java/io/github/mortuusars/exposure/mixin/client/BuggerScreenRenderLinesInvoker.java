package io.github.mortuusars.exposure.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public interface BuggerScreenRenderLinesInvoker {
    /** 26.3 added the screen width; passing {@code guiWidth()} keeps the 26.1.2 right-side alignment. */
    @Invoker("extractLines")
    void drawLines(GuiGraphicsExtractor guiGraphics, List<String> lines, boolean leftSide, int scaledScreenWidth);
}
