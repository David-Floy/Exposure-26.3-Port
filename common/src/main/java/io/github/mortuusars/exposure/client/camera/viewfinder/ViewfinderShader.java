package io.github.mortuusars.exposure.client.camera.viewfinder;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.client.util.Shader;
import io.github.mortuusars.exposure.data.Filter;
import io.github.mortuusars.exposure.data.Filters;
import io.github.mortuusars.exposure.world.camera.Camera;
import io.github.mortuusars.exposure.world.item.camera.Attachment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ViewfinderShader implements AutoCloseable {
    private final Minecraft minecraft;
    private final Camera camera;
    private final Viewfinder viewfinder;

    private @Nullable Identifier shaderLocation;
    private boolean active;

    public ViewfinderShader(Camera camera, Viewfinder viewfinder) {
        this.minecraft = Minecrft.get();
        this.camera = camera;
        this.viewfinder = viewfinder;
        this.update();
    }

    public void apply(Identifier location) {
        RenderSystem.assertOnRenderThread();
        if (location.equals(shaderLocation)) {
            return;
        }

        shaderLocation = null;
        if (Shader.isAvailable(location)) {
            shaderLocation = location;
            active = true;
        } else {
            Exposure.LOGGER.warn("Failed to select viewfinder post effect '{}'.", location);
            active = false;
        }
    }

    /** Processes the current viewfinder post effect when it is present and active. */
    public void process() {
        Identifier location = shaderLocation;
        if (location != null && active) {
            Shader.process(location, minecraft.getMainRenderTarget());
        }
    }

    public void update() {
        setActive(viewfinder.isLookingThrough());
        if (active) {
            ItemStack filterStack = Attachment.FILTER.get(camera.getItemStack()).getForReading();
            Filters.of(Minecrft.registryAccess(), filterStack).map(Filter::shader).ifPresentOrElse(this::apply, this::remove);
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void remove() {
        RenderSystem.assertOnRenderThread();
        shaderLocation = null;
    }

    @Override
    public void close() {
        remove();
    }
}
