package io.github.mortuusars.exposure.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;

import java.util.function.*;

public record KeyBinding(Key matcher, Supplier<Boolean> handler) {
    public boolean keyPressed(KeyEvent event) {
        return keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return matchesPress(keyCode, scanCode, modifiers) && handler().get();
    }

    public boolean keyReleased(KeyEvent event) {
        return keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return matchesRelease(keyCode, scanCode, modifiers) && handler().get();
    }

    public boolean matches(int keyCode, int scanCode, int press, int modifiers) {
        return matcher.matches(keyCode, scanCode, press, modifiers);
    }

    public boolean matchesPress(int keyCode, int scanCode, int modifiers) {
        return matcher.matches(keyCode, scanCode, InputConstants.PRESS, modifiers);
    }

    public boolean matchesRelease(int keyCode, int scanCode, int modifiers) {
        return matcher.matches(keyCode, scanCode, InputConstants.RELEASE, modifiers);
    }
}
