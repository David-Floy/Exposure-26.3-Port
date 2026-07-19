package io.github.mortuusars.exposure.world.item;

import io.github.mortuusars.exposure.Exposure;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class BrokenInterplanarProjectorItem extends Item {
    public BrokenInterplanarProjectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay tooltipDisplay,
                                java.util.function.Consumer<Component> tooltipConsumer, TooltipFlag tooltipFlag) {
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            tooltipConsumer.accept(Component.translatable("item.exposure.broken_interplanar_projector.tooltip.broken").withStyle(ChatFormatting.DARK_RED));
        }
    }

    public String getErrorCode(ItemStack stack) {
        return stack.getOrDefault(Exposure.DataComponents.INTERPLANAR_PROJECTOR_ERROR_CODE, "ERR_FAILURE_SEE_LOGS");
    }
}
