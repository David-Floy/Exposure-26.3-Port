package io.github.mortuusars.exposure.client.animation;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class CameraModelPoses {
    public static final HashMap<Identifier, CameraPoses> POSES = new HashMap<>();

    public static final CameraPoses DEFAULT = new CameraPoses();

    public static void register(CameraItem item, CameraPoses poses) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        Preconditions.checkArgument(!POSES.containsKey(itemId), "CameraPoses for item: '" + itemId + "' already registered.");
        POSES.put(itemId, poses);
    }

    public static @NotNull CameraPoses get(Identifier itemId) {
        return POSES.getOrDefault(itemId, DEFAULT);
    }
}
