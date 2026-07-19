package io.github.mortuusars.exposure.commands.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.arguments.IdentifierArgument;

import java.util.concurrent.CompletableFuture;

public class TextureLocationArgument extends IdentifierArgument {
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        // This is part of the server command tree. Referencing Minecraft here class-loads
        // client-only code when a dedicated server asks Brigadier for suggestions.
        return Suggestions.empty();
    }
}
