package io.github.mortuusars.exposure.client.render.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedTransformCompositeModelTests {
    @Test
    void attachmentLayersUseTheDisplayTransformOfTheFirstLayer() {
        ItemTransform body = translatedUp(5);
        ItemStackRenderState state = new ItemStackRenderState();

        // The item stack and resolver are only forwarded to the sub-models, which ignore them here.
        new SharedTransformCompositeModel(List.of(new LayerModel(body), new LayerModel(ItemTransform.NO_TRANSFORM),
                new LayerModel(translatedUp(2))))
                .update(state, null, null, ItemDisplayContext.GROUND, null, null, 0);

        assertEquals(3, state.activeLayerCount);
        for (int i = 0; i < state.activeLayerCount; i++) {
            assertSame(body, state.layers[i].itemTransform, "Layer " + i + " must use the camera body transform");
        }
    }

    @Test
    void layersAddedBeforeTheCompositeKeepTheirTransforms() {
        ItemTransform previous = translatedUp(1);
        ItemTransform body = translatedUp(5);
        ItemStackRenderState state = new ItemStackRenderState();
        state.newLayer().setItemTransform(previous);

        new SharedTransformCompositeModel(List.of(new LayerModel(body), new LayerModel(ItemTransform.NO_TRANSFORM)))
                .update(state, null, null, ItemDisplayContext.GROUND, null, null, 0);

        assertEquals(3, state.activeLayerCount);
        assertSame(previous, state.layers[0].itemTransform);
        assertSame(body, state.layers[1].itemTransform);
        assertSame(body, state.layers[2].itemTransform);
    }

    @Test
    void cameraItemDefinitionAddsAttachmentsWithTheCameraBodyTransform() throws IOException {
        JsonElement definition;
        try (InputStream stream = SharedTransformCompositeModelTests.class.getResourceAsStream("/assets/exposure/items/camera.json")) {
            assertNotNull(stream, "Missing camera item definition");
            definition = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }

        List<JsonObject> composites = new ArrayList<>();
        collectComposites(definition, composites);

        assertEquals(3, composites.size(), "Default, active and selfie camera models combine attachments");
        for (JsonObject composite : composites) {
            assertEquals("exposure:shared_transform_composite", composite.get("type").getAsString(),
                    "minecraft:composite applies each attachment model's own (missing) display transforms");

            JsonArray models = composite.getAsJsonArray("models");
            assertFalse(models.get(0).toString().contains("camera_parts/"), "The first layer must be the camera body");
            for (int i = 1; i < models.size(); i++) {
                assertTrue(models.get(i).toString().contains("camera_parts/"), "Only attachments follow the camera body");
            }
        }
    }

    private static void collectComposites(JsonElement element, List<JsonObject> composites) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectComposites(child, composites));
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("type") && object.get("type").getAsString().endsWith("composite")) {
                composites.add(object);
            }
            object.entrySet().forEach(entry -> collectComposites(entry.getValue(), composites));
        }
    }

    private static ItemTransform translatedUp(float pixels) {
        return new ItemTransform(new Vector3f(), new Vector3f(0, pixels / 16F, 0), new Vector3f(0.9F, 0.9F, 0.9F));
    }

    private record LayerModel(ItemTransform transform) implements ItemModel {
        @Override
        public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext,
                           @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
            output.newLayer().setItemTransform(this.transform);
        }
    }
}
