package io.github.mortuusars.exposure.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.exposure.client.camera.CameraClient;
import io.github.mortuusars.exposure.client.camera.viewfinder.Viewfinder;
import io.github.mortuusars.exposure.client.util.Minecrft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public class MouseHandler {
    private static final boolean[] heldMouseButtons = new boolean[12];

    public static boolean isMouseButtonHeld(int button) {
        return button >= 0 && button < heldMouseButtons.length && heldMouseButtons[button];
    }

    public static boolean buttonPressed(MouseButtonInfo buttonInfo, int action) {
        int button = buttonInfo.button();
        if (button >= 0 && button < heldMouseButtons.length)
            heldMouseButtons[button] = action == InputConstants.PRESS;

        var minecraft = Minecrft.get();
        var window = minecraft.getWindow();
        MouseButtonEvent event = new MouseButtonEvent(
                minecraft.mouseHandler.getScaledXPos(window),
                minecraft.mouseHandler.getScaledYPos(window),
                buttonInfo);
        return CameraClient.viewfinder() != null && CameraClient.viewfinder().mouseClicked(event, action);
    }

    public static boolean scrolled(double amount) {
        return CameraClient.viewfinder() != null && CameraClient.viewfinder().mouseScrolled(amount);
    }

    public static double modifySensitivity(double original) {
        return CameraClient.viewfinder() != null ? CameraClient.viewfinder().modifyMouseSensitivity(original) : original;
    }

    public static boolean onTurnPlayer(double xRot, double yRot) {
        return CameraClient.viewfinder() != null
                && CameraClient.viewfinder().isLookingThrough()
                && CameraClient.viewfinder().selfie().mouseMove(xRot, yRot);
    }
}
