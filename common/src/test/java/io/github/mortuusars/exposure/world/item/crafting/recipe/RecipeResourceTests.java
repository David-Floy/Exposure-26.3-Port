package io.github.mortuusars.exposure.world.item.crafting.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeResourceTests {
    @Test
    void albumKeepsOriginalShapelessSemanticsWithTwoCurrentCodecIngredients() {
        JsonObject recipe = recipe("album");

        assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());
        assertStringArray(recipe.getAsJsonArray("ingredients"), List.of(
                "minecraft:writable_book",
                "minecraft:phantom_membrane"));
        assertEquals("exposure:album", recipe.getAsJsonObject("result").get("id").getAsString());
    }

    @Test
    void customComponentTransferringRecipesRetainTheirExactSourceAndExtraConstraints() {
        JsonObject projector = recipe("broken_interplanar_projector_fixing");
        assertEquals("exposure:broken_interplanar_projector", projector.get("source").getAsString());
        assertStringArray(projector.getAsJsonArray("ingredients"), List.of("minecraft:ender_eye"));
        assertEquals("exposure:interplanar_projector",
                projector.getAsJsonObject("result").get("id").getAsString());

        JsonObject aging = recipe("photograph_aging");
        assertEquals("exposure:photograph", aging.get("photograph").getAsString());
        assertStringArray(aging.getAsJsonArray("ingredients"), List.of(
                "#exposure:photo_agers",
                "minecraft:brush"));
        assertEquals("exposure:aged_photograph", aging.getAsJsonObject("result").get("id").getAsString());

        JsonObject copying = recipe("photograph_copying_color");
        assertEquals("exposure:photograph", copying.get("photograph").getAsString());
        assertStringArray(copying.getAsJsonArray("ingredients"), List.of(
                "#exposure:photo_papers",
                "#exposure:black_printing_dyes",
                "#exposure:cyan_printing_dyes",
                "#exposure:magenta_printing_dyes",
                "#exposure:yellow_printing_dyes"));
        assertEquals("exposure:photograph", copying.getAsJsonObject("result").get("id").getAsString());
    }

    private static JsonObject recipe(String id) {
        String path = "/data/exposure/recipe/" + id + ".json";
        InputStream stream = RecipeResourceTests.class.getResourceAsStream(path);
        assertNotNull(stream, "Missing recipe resource: " + path);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new AssertionError("Cannot read recipe resource: " + path, exception);
        }
    }

    private static void assertStringArray(JsonArray actual, List<String> expected) {
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            assertTrue(actual.get(index).isJsonPrimitive());
            assertEquals(expected.get(index), actual.get(index).getAsString());
        }
    }
}
