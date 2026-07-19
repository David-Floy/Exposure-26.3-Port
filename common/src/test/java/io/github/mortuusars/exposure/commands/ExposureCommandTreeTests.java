package io.github.mortuusars.exposure.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.tree.CommandNode;
import io.github.mortuusars.exposure.commands.argument.ExposureLookArgument;
import io.github.mortuusars.exposure.commands.argument.SizeMultiplierArgument;
import io.github.mortuusars.exposure.data.export.ExportLook;
import io.github.mortuusars.exposure.data.export.ExportSize;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExposureCommandTreeTests {
    @Test
    void productionRegistrationRetainsTheSupportedCommandTree() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        ExposureCommand.register(dispatcher);

        CommandNode<CommandSourceStack> exposure = dispatcher.getRoot().getChild("exposure");
        assertEquals(Set.of("load", "expose", "export", "show", "palette", "debug"), childNames(exposure));

        CommandNode<CommandSourceStack> export = exposure.getChild("export");
        assertEquals(Set.of("id", "all", "stop"), childNames(export));
        assertEquals(Set.of("id"), childNames(export.getChild("id")));
        assertEquals(Set.of("size"), childNames(export.getChild("id").getChild("id")));
        assertEquals(Set.of("look"), childNames(export.getChild("id").getChild("id").getChild("size")));
        assertEquals(Set.of("size"), childNames(export.getChild("all")));
        assertEquals(Set.of("look"), childNames(export.getChild("all").getChild("size")));

        assertEquals(Set.of("latest", "id", "texture"), childNames(exposure.getChild("show")));
        assertEquals(Set.of("export", "convert"), childNames(exposure.getChild("palette")));
        assertEquals(Set.of("clear_rendering_cache", "highlight_entities_in_frame", "expose_rgb",
                "chromatic_from_last_three_exposures", "develop_film_in_hand"), childNames(exposure.getChild("debug")));
    }

    @Test
    void exportArgumentsParseEveryRegisteredValueAndRejectUnknownValues() throws Exception {
        SizeMultiplierArgument size = new SizeMultiplierArgument();
        for (ExportSize value : ExportSize.values()) {
            assertEquals(value, size.parse(new StringReader(value.getSerializedName())));
        }
        assertThrows(Exception.class, () -> size.parse(new StringReader("x7")));

        ExposureLookArgument look = new ExposureLookArgument();
        for (ExportLook value : ExportLook.values()) {
            assertEquals(value, look.parse(new StringReader(value.getSerializedName())));
        }
        assertThrows(Exception.class, () -> look.parse(new StringReader("sepia")));
    }

    private static Set<String> childNames(CommandNode<?> node) {
        return node.getChildren().stream().map(CommandNode::getName).collect(Collectors.toSet());
    }
}
