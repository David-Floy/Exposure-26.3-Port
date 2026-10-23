package io.github.mortuusars.exposure.client.input;

import org.lwjgl.sdl.SDLKeycode;

/**
 * Logical modifier flags used by {@link Key} bindings.
 */
public interface Modifier {
    int NONE = 0;
    int SHIFT = 0x1;
    int CONTROL = 0x2;
    int ALT = 0x4;
    int SUPER = 0x8;

    /**
     * Converts the modifier mask of a 26.3 input event to the flags above. SDL reports left and right keys as
     * separate bits and always includes lock-key state (Num Lock, Caps Lock); GLFW in 26.1.2 merged both sides
     * and reported no lock keys, so bindings without modifiers kept working with Num Lock enabled.
     */
    static int fromInput(int modifiers) {
        int result = NONE;
        if ((modifiers & SDLKeycode.SDL_KMOD_SHIFT) != 0) result |= SHIFT;
        if ((modifiers & SDLKeycode.SDL_KMOD_CTRL) != 0) result |= CONTROL;
        if ((modifiers & SDLKeycode.SDL_KMOD_ALT) != 0) result |= ALT;
        if ((modifiers & SDLKeycode.SDL_KMOD_GUI) != 0) result |= SUPER;
        return result;
    }
}
