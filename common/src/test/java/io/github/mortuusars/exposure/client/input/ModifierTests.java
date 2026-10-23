package io.github.mortuusars.exposure.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;
import org.lwjgl.sdl.SDLKeycode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierTests {
    @Test
    void lockKeysDoNotPreventBindingsWithoutModifiers() {
        int numAndCapsLock = SDLKeycode.SDL_KMOD_NUM | SDLKeycode.SDL_KMOD_CAPS;

        assertEquals(Modifier.NONE, Modifier.fromInput(numAndCapsLock));
        assertTrue(Key.press(InputConstants.KEY_ADD)
                .matches(InputConstants.KEY_ADD, 0, InputConstants.PRESS, numAndCapsLock));
    }

    @Test
    void leftAndRightModifierKeysAreEquivalent() {
        assertEquals(Modifier.CONTROL, Modifier.fromInput(SDLKeycode.SDL_KMOD_LCTRL));
        assertEquals(Modifier.CONTROL, Modifier.fromInput(SDLKeycode.SDL_KMOD_RCTRL));
        assertEquals(Modifier.CONTROL | Modifier.SHIFT,
                Modifier.fromInput(SDLKeycode.SDL_KMOD_RCTRL | SDLKeycode.SDL_KMOD_LSHIFT));
        assertEquals(Modifier.ALT | Modifier.SUPER,
                Modifier.fromInput(SDLKeycode.SDL_KMOD_RALT | SDLKeycode.SDL_KMOD_LGUI));
    }

    @Test
    void shortcutsRequireExactlyTheirModifiers() {
        Key copy = Key.press(Modifier.CONTROL, InputConstants.KEY_C);

        assertTrue(copy.matches(InputConstants.KEY_C, 0, InputConstants.PRESS, SDLKeycode.SDL_KMOD_LCTRL));
        assertFalse(copy.matches(InputConstants.KEY_C, 0, InputConstants.PRESS,
                SDLKeycode.SDL_KMOD_LCTRL | SDLKeycode.SDL_KMOD_LSHIFT));
        assertFalse(copy.matches(InputConstants.KEY_C, 0, InputConstants.PRESS, SDLKeycode.SDL_KMOD_NONE));
    }
}
