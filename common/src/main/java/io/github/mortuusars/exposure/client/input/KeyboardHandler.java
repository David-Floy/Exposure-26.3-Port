package io.github.mortuusars.exposure.client.input;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.client.camera.CameraClient;
import io.github.mortuusars.exposure.client.util.Minecrft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class KeyboardHandler {
    @Nullable
    private static KeyMapping openCameraControlsKey = null;
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Exposure.resource("main"));

    public static void registerKeymappings(Function<KeyMapping, KeyMapping> registerFunction) {
        KeyMapping keyMapping = new KeyMapping("key.exposure.camera_controls",
                InputConstants.UNKNOWN.getValue(), CATEGORY);

        openCameraControlsKey = registerFunction.apply(keyMapping);
    }

    public static boolean handleKeyPress(long windowId, int action, KeyEvent event) {
        return Minecrft.get().player != null
                && CameraClient.viewfinder() != null
                && CameraClient.viewfinder().keyPressed(event, action);
    }

    public static KeyMapping getCameraControlsKey() {
        Preconditions.checkState(openCameraControlsKey != null,
                "Viewfinder Controls key mapping was not registered");

        return openCameraControlsKey.isUnbound() ? Minecraft.getInstance().options.keyShift : openCameraControlsKey;
    }
}
