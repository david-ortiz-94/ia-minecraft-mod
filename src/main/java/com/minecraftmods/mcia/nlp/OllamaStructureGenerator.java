package com.minecraftmods.mcia.nlp;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.models.chat.*;
import com.google.gson.*;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class OllamaStructureGenerator {
    private static final String MODEL_NAME = "llama3.2:latest";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final OllamaAPI ollama = new OllamaAPI("http://localhost:11434/");
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_TIMEOUT_SECONDS = 60;
    private static final long BACKOFF_MULTIPLIER = 2;
    private static final Map<String, String> BLOCK_CORRECTIONS = Map.of(
            "wooden_door", "oak_door",
            "cedar_log", "spruce_log",
            "spruce_logs", "spruce_log",
            "red_stone_bricks", "red_bricks",
            "moisture_resistant_cobblestone", "mossy_cobblestone"
    );
    public OllamaStructureGenerator() {
        configureOllama();
    }

    private void configureOllama() {
        ollama.setRequestTimeoutSeconds(INITIAL_TIMEOUT_SECONDS);
        ollama.setVerbose(false);
    }

    public static CompletableFuture<Clipboard> generateStructureAsync(String description) {
        return CompletableFuture.supplyAsync(() -> {
            int attempt = 0;
            Exception lastException = null;

            while (attempt < MAX_RETRIES) {
                try {
                    // Calculate timeout with exponential backoff
                    long timeout = INITIAL_TIMEOUT_SECONDS * (long) Math.pow(BACKOFF_MULTIPLIER, attempt);
                    ollama.setRequestTimeoutSeconds(timeout);

                    // 1. Get structure design
                    JsonObject structureDesign = getStructureDesign(description);

                    // 2. Create clipboard
                    Clipboard clipboard = createClipboard(structureDesign);

                    // 3. Build structure
                    buildStructure(clipboard, structureDesign);

                    return clipboard;
                } catch (Exception e) {
                    lastException = e;
                    attempt++;
                    if (attempt < MAX_RETRIES) {
                        try {
                            Thread.sleep(1000 * attempt); // Wait before retry
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new CompletionException("Interrupted during retry", ie);
                        }
                    }
                }
            }

            throw new CompletionException("Failed after " + MAX_RETRIES + " attempts", lastException);
        }, executor);
    }

    private static JsonObject getStructureDesign(String description) throws Exception {
        String systemPrompt = """
Eres un generador de estructuras para Minecraft 1.21.5. Reglas estrictas:
1. Usa SOLO bloques existentes en Minecraft 1.21.5 (ej: minecraft:oak_planks, minecraft:stone_bricks)
2. MÍNIMO 15 bloques sólidos (no usar solo vidrios o plantas)
3. Incluir al menos 3 tipos diferentes de bloques estructurales
4. La estructura debe tener volumen (ancho, alto y profundidad >= 3)
5. NO uses bloques como wooden_door o cedar_log - usa oak_door y spruce_log en su lugar
6. La respuesta debe ser EXCLUSIVAMENTE un objeto JSON válido
7. No incluyas ningún texto adicional fuera del JSON
8. Ejemplo de JSON válido:
{
  "dimensions": {"width": 5, "height": 4, "length": 5},
  "blocks": {
    "0,0,0": "minecraft:oak_planks",
    "0,1,0": "minecraft:oak_log",
    "1,0,0": "minecraft:stone_bricks",
    "0,0,1": "minecraft:glass_pane",
    // ... mínimo 15 bloques
  },
  "metadata": {
    "type": "house",
    "style": "modern"
  }
}
9. ¡NO USAR SOLO VIDRIOS! Combinar con bloques estructurales
""";

        OllamaChatRequestModel request = getOllamaChatRequestModel(description, systemPrompt);

        OllamaChatResult result = ollama.chat(request);
        return parseResponse(result);
    }

    private static @NotNull OllamaChatRequestModel getOllamaChatRequestModel(String description, String systemPrompt) {
        OllamaChatMessage systemMessage = new OllamaChatMessage(
                OllamaChatMessageRole.SYSTEM, systemPrompt
        );

        OllamaChatMessage userMessage = new OllamaChatMessage(
                OllamaChatMessageRole.USER, description
        );
        List<OllamaChatMessage> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);

        OllamaChatRequestModel request = new OllamaChatRequestModel(MODEL_NAME, messages);
        Map<String, Object> options = new HashMap<>();
        options.put("format", "json");
        request.setOptions(options);
        return request;
    }

    private static JsonObject parseResponse(OllamaChatResult result) {
        try {
            if (result == null) {
                throw new RuntimeException("La respuesta de Ollama es nula");
            }

            String rawResponse = result.getResponse();
            if (rawResponse == null || rawResponse.trim().isEmpty()) {
                throw new RuntimeException("La respuesta de Ollama está vacía");
            }

            // Extraer solo el contenido JSON del mensaje
            String jsonContent = extractJsonContent(rawResponse);
            if (jsonContent == null) {
                throw new RuntimeException("No se encontró contenido JSON válido en la respuesta");
            }

            // Parsear el JSON
            JsonElement jsonElement = gson.fromJson(jsonContent, JsonElement.class);
            if (jsonElement == null || !jsonElement.isJsonObject()) {
                throw new RuntimeException("El contenido JSON no es un objeto válido");
            }

            return jsonElement.getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar respuesta de Ollama: " + e.getMessage(), e);
        }
    }

    private static String extractJsonContent(String rawResponse) {
        // Caso 1: Respuesta contiene ```json ... ```
        if (rawResponse.contains("```json")) {
            int start = rawResponse.indexOf("```json") + "```json".length();
            int end = rawResponse.lastIndexOf("```");
            if (start >= 0 && end > start) {
                return rawResponse.substring(start, end).trim();
            }
        }

        // Caso 2: Respuesta es directamente un objeto JSON
        if (rawResponse.trim().startsWith("{") && rawResponse.trim().endsWith("}")) {
            return rawResponse.trim();
        }

        // Caso 3: Buscar el primer objeto JSON en el texto
        int jsonStart = rawResponse.indexOf('{');
        int jsonEnd = rawResponse.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return rawResponse.substring(jsonStart, jsonEnd + 1);
        }

        return null;
    }

    private static Clipboard createClipboard(JsonObject design) throws Exception {
        JsonObject dims = design.getAsJsonObject("dimensions");
        BlockVector3 min = BlockVector3.at(0, 0, 0);
        BlockVector3 max = BlockVector3.at(
                dims.get("width").getAsInt() - 1,
                dims.get("height").getAsInt() - 1,
                dims.get("length").getAsInt() - 1
        );
        return new BlockArrayClipboard(new CuboidRegion(min, max));
    }

    private static void buildStructure(Clipboard clipboard, JsonObject design) {
        JsonObject blocks = design.getAsJsonObject("blocks");
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(clipboard.getRegion().getWorld())) {
            blocks.entrySet().forEach(entry -> {
                String[] coords = entry.getKey().split(",");
                if (coords.length != 3) {
                    throw new RuntimeException("Coordenadas inválidas: " + entry.getKey());
                }
                BlockVector3 pos = BlockVector3.at(
                        Integer.parseInt(coords[0]),
                        Integer.parseInt(coords[1]),
                        Integer.parseInt(coords[2])
                );
                try {
                    editSession.setBlock(pos, parseBlock(entry.getValue().getAsString()));
                } catch (MaxChangedBlocksException  e) {
                    throw new RuntimeException("Error al colocar bloque en " + entry.getKey() +
                            " (" + entry.getValue().getAsString() + "): " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Error al construir estructura: " + e.getMessage(), e);
        }
    }
    private static BlockState parseBlock(String blockId) {
        // Aplicar correcciones primero
        String correctedId = BLOCK_CORRECTIONS.getOrDefault(
                blockId.replace("minecraft:", ""),
                blockId
        );

        // Eliminar propiedades adicionales
        String baseBlockId = correctedId.split("\\[")[0];

        BlockType blockType = BlockTypes.get(baseBlockId);
        if (blockType == null) {
            throw new IllegalArgumentException("Bloque no encontrado: " + baseBlockId);
        }

        return blockType.getDefaultState();
    }
    private static BlockState parseBlock2(String blockId) {
        try {
            // Elimina propiedades adicionales (ej: "minecraft:oak_planks[axis=x]" → "minecraft:oak_planks")
            String baseBlockId = blockId.split("\\[")[0];
            // Mapeo de bloques sugeridos por Ollama a bloques válidos
            Map<String, String> blockMappings = Map.of(
                    "iron_brick", "minecraft:iron_block",
                    "dark_stone", "minecraft:blackstone",
                    "wooden_door", "minecraft:oak_door",
                    "cedar_log", "minecraft:spruce_log"
            );
            // Reemplaza bloques inválidos
            // Reemplaza bloques inválidos
            baseBlockId = blockMappings.getOrDefault(
                    baseBlockId.replace("minecraft:", ""),
                    baseBlockId
            );


            // Obtiene el tipo de bloque (ej: "minecraft:stone")
            BlockType blockType = BlockTypes.get(baseBlockId);
            if (blockType == null) {
                throw new IllegalArgumentException("Bloque no encontrado: " + baseBlockId);
            }

            // Retorna el estado predeterminado del bloque
            return blockType.getDefaultState();
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear bloque: " + blockId, e);
        }
    }
}