package io.github.mortuusars.exposure.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 26.3 data codecs ignore unknown keys, so datapack JSON in the 26.1.2 format still loads without an error but
 * silently loses its loot functions, conditions and predicates. These checks keep the legacy keys out.
 */
class DataResourceFormatTests {
    @Test
    void lootTablesUseSingleConditionAndModifierHolders() throws IOException {
        List<String> problems = new ArrayList<>();
        List<Path> files = dataFiles("loot_table");
        for (Path file : files) {
            visit(read(file), (object, path) -> {
                for (String legacyKey : List.of("conditions", "functions", "function")) {
                    if (object.has(legacyKey)) problems.add(file.getFileName() + " " + path + " has '" + legacyKey + "'");
                }
            }, "$");
        }
        assertFalse(files.isEmpty(), "No loot tables found");
        assertEquals(List.of(), problems);
    }

    @Test
    void advancementPredicatesUseTheTypedLootConditionFormat() throws IOException {
        List<String> problems = new ArrayList<>();
        List<Path> files = dataFiles("advancement");
        for (Path file : files) {
            visit(read(file), (object, path) -> {
                if (object.has("conditions") && !object.has("trigger")) {
                    problems.add(file.getFileName() + " " + path + " has a legacy condition list");
                }
                if (object.has("condition")) {
                    problems.add(file.getFileName() + " " + path + " uses 'condition' instead of 'type'");
                }
                if (object.has("type_specific")) {
                    problems.add(file.getFileName() + " " + path + " has 'type_specific'");
                }
                if (object.has("tag") && !object.has("items")) {
                    problems.add(file.getFileName() + " " + path + " has an item predicate 'tag' instead of '#items'");
                }
                if ("minecraft:recipe_unlocked".equals(string(object, "trigger"))
                        && object.getAsJsonObject("conditions").has("recipe")) {
                    problems.add(file.getFileName() + " " + path + " uses 'recipe' instead of 'recipes'");
                }
                if ("exposure:frame_exposed".equals(string(object, "trigger"))) {
                    JsonElement entities = object.getAsJsonObject("conditions").get("entities_in_frame");
                    if (entities != null) {
                        for (JsonElement entity : entities.getAsJsonArray()) {
                            String type = entity.isJsonObject() ? string(entity.getAsJsonObject(), "type") : null;
                            if (type == null || !type.startsWith("minecraft:")) {
                                problems.add(file.getFileName() + " " + path + " entities_in_frame entry is not a loot condition: " + entity);
                            }
                        }
                    }
                }
            }, "$");
        }
        assertTrue(files.size() >= 28, "Expected all Exposure advancements, found " + files.size());
        assertEquals(List.of(), problems);
    }

    @Test
    void entityPropertyConditionsUseSubPredicateMaps() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Path file : dataFiles("advancement")) {
            visit(read(file), (object, path) -> {
                if ("minecraft:entity_properties".equals(string(object, "type")) && object.has("predicate")) {
                    for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("predicate").entrySet()) {
                        if (entry.getKey().indexOf(':') < 0) {
                            problems.add(file.getFileName() + " " + path + " predicate key '" + entry.getKey()
                                    + "' is not a sub-predicate type id");
                        }
                    }
                }
            }, "$");
        }
        assertEquals(List.of(), problems);
    }

    private interface Visitor {
        void accept(JsonObject object, String path);
    }

    private static void visit(JsonElement element, Visitor visitor, String path) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            visitor.accept(object, path);
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                visit(entry.getValue(), visitor, path + "." + entry.getKey());
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                visit(array.get(i), visitor, path + "[" + i + "]");
            }
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    /** All files of a data folder in both the common and the Fabric module. */
    private static List<Path> dataFiles(String folder) throws IOException {
        Path root = repositoryRoot();
        List<Path> files = new ArrayList<>();
        for (String module : List.of("common", "fabric")) {
            Path data = root.resolve(module).resolve("src/main/resources/data");
            if (!Files.isDirectory(data)) continue;
            try (Stream<Path> namespaces = Files.list(data)) {
                for (Path namespace : namespaces.toList()) {
                    Path dir = namespace.resolve(folder);
                    if (!Files.isDirectory(dir)) continue;
                    try (Stream<Path> walk = Files.walk(dir)) {
                        walk.filter(p -> p.toString().endsWith(".json")).sorted().forEach(files::add);
                    }
                }
            }
        }
        return files;
    }

    private static Path repositoryRoot() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        if (dir == null) throw new IllegalStateException("Cannot locate the repository root");
        return dir;
    }

    private static JsonElement read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
    }
}
