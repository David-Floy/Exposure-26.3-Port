package io.github.mortuusars.exposure.world.item;

import io.github.mortuusars.exposure.world.camera.ExposureType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DevelopedFilmItem extends Item implements FilmItem {
    private final ExposureType type;

    public DevelopedFilmItem(ExposureType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public ExposureType getType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay tooltipDisplay,
                                java.util.function.Consumer<Component> tooltipConsumer, TooltipFlag tooltipFlag) {
        int exposedFrames = getStoredFramesCount(stack);
        if (exposedFrames > 0) {
            tooltipConsumer.accept(Component.translatable("item.exposure.developed_film.tooltip.frame_count", exposedFrames)
                    .withStyle(ChatFormatting.GRAY));
        }

        int frameSize = getFrameSize(stack);
        if (frameSize != getDefaultFrameSize(stack)) {
            tooltipConsumer.accept(Component.translatable("item.exposure.film_roll.tooltip.frame_size",
                            Component.literal(String.format("%.1f", frameSize / 10f)))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
