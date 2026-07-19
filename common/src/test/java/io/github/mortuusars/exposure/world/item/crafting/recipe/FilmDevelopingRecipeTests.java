package io.github.mortuusars.exposure.world.item.crafting.recipe;

import io.github.mortuusars.exposure.world.camera.frame.Frame;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilmDevelopingRecipeTests {
    private static final DataComponentType<List<Frame>> FILM_FRAMES = DataComponentType.<List<Frame>>builder()
            .persistent(Frame.CODEC.listOf(0, 256))
            .build();

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Items.PAPER.builtInRegistryHolder().bindComponents(identityComponents("paper"));
        Items.POTION.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        Items.GLASS_BOTTLE.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        Items.FILLED_MAP.builtInRegistryHolder().bindComponents(identityComponents("filled_map"));
        Items.BOOK.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }

    private static DataComponentMap identityComponents(String itemPath) {
        return DataComponentMap.builder()
                .set(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace(itemPath))
                .set(DataComponents.ITEM_NAME, Component.translatable("item.minecraft." + itemPath))
                .build();
    }

    private static FilmDevelopingRecipe recipe() {
        NonNullList<Ingredient> chemicals = NonNullList.create();
        chemicals.add(Ingredient.of(Items.POTION));
        return new FilmDevelopingRecipe(
                CraftingBookCategory.MISC,
                Ingredient.of(Items.PAPER),
                chemicals,
                new ItemStackTemplate(Items.FILLED_MAP));
    }

    @Test
    void exposedFilmMatchesAndTransfersItsFramesToDevelopedResult() {
        List<Frame> frames = List.of(Frame.EMPTY);
        ItemStack exposedFilm = new ItemStack(Items.PAPER);
        exposedFilm.set(FILM_FRAMES, frames);
        ItemStack waterPotion = new ItemStack(Items.POTION);
        CraftingInput input = CraftingInput.of(2, 1, List.of(exposedFilm, waterPotion));

        assertTrue(recipe().matches(input, null));

        ItemStack result = recipe().assemble(input);
        assertTrue(result.is(Items.FILLED_MAP));
        assertEquals(1, result.getCount());
        assertEquals(frames, result.get(FILM_FRAMES));
        assertEquals(recipe().getResultItem().get(DataComponents.ITEM_MODEL),
                result.get(DataComponents.ITEM_MODEL));
        assertEquals(recipe().getResultItem().get(DataComponents.ITEM_NAME),
                result.get(DataComponents.ITEM_NAME));
        assertNotEquals(exposedFilm.get(DataComponents.ITEM_MODEL),
                result.get(DataComponents.ITEM_MODEL));
        assertTrue(exposedFilm.is(Items.PAPER));
        assertEquals(frames, exposedFilm.get(FILM_FRAMES));
        assertTrue(recipe().getRemainingItems(input).get(1).is(Items.GLASS_BOTTLE));
    }

    @Test
    void invalidDevelopingInputsRemainRejected() {
        ItemStack exposedFilm = new ItemStack(Items.PAPER);
        exposedFilm.set(FILM_FRAMES, List.of(Frame.EMPTY));

        CraftingInput missingChemical = CraftingInput.of(2, 1,
                List.of(exposedFilm, ItemStack.EMPTY));
        CraftingInput wrongSource = CraftingInput.of(2, 1,
                List.of(new ItemStack(Items.BOOK), new ItemStack(Items.POTION)));
        CraftingInput extraIngredient = CraftingInput.of(3, 1,
                List.of(exposedFilm, new ItemStack(Items.POTION), new ItemStack(Items.BOOK)));

        assertFalse(recipe().matches(missingChemical, null));
        assertFalse(recipe().matches(wrongSource, null));
        assertFalse(recipe().matches(extraIngredient, null));
    }
}
