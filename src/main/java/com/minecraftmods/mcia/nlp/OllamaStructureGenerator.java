package com.minecraftmods.mcia.nlp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minecraftmods.mcia.generation.models.StructureDesign;
import com.minecraftmods.mcia.utils.PromptUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatMessage;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatMessageRole;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatRequestModel;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OllamaStructureGenerator {
    private static final String MODEL_NAME = "llama3.2:latest";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final OllamaAPI ollama = new OllamaAPI("http://localhost:11434/");
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_TIMEOUT_SECONDS = 90;
    private static final long BACKOFF_MULTIPLIER = 2;

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
                    long timeout = INITIAL_TIMEOUT_SECONDS * (long) Math.pow(BACKOFF_MULTIPLIER, attempt);
                    ollama.setRequestTimeoutSeconds(timeout); // ← Aumenta timeout en cada intento

                    StructureDesign design = getStructureDesign(description);
                    return buildClipboard(design.getRawDesign());

                } catch (HttpTimeoutException e) {
                    lastException = e;
                    attempt++;
                    if (attempt < MAX_RETRIES) {
                        try {
                            Thread.sleep(1000L * attempt); // Backoff exponencial
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new CompletionException(ie);
                        }
                    }
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }

            throw new CompletionException("Falló después de " + MAX_RETRIES + " intentos", lastException);
        }, executor);
    }


    private static StructureDesign getStructureDesign(String description) throws Exception {


        OllamaChatRequestModel request = getOllamaChatRequestModel(description);

        OllamaChatResult result = ollama.chat(request);
        JsonObject response = parseResponse(result);

        // Validación básica del JSON antes de continuar
        if (!response.has("dimensions") || !response.has("blocks")) {
            throw new IOException("El JSON no contiene dimensions o blocks");
        }

        improveStructure(response); // Aplicar post-procesamiento
        return new StructureDesign(response);
    }

    private static void improveStructure(JsonObject design) {
        // Asegurar dimensiones mínimas
        JsonObject dims = design.getAsJsonObject("dimensions");
        dims.addProperty("width", Math.max(7, dims.get("width").getAsInt()));
        dims.addProperty("height", Math.max(5, dims.get("height").getAsInt()));
        dims.addProperty("length", Math.max(7, dims.get("length").getAsInt()));

        // Añadir piso si no existe
        for(int x=0; x<dims.get("width").getAsInt(); x++) {
            for(int z=0; z<dims.get("length").getAsInt(); z++) {
                String key = x+",0,"+z;
                if(!design.getAsJsonObject("blocks").has(key)) {
                    design.getAsJsonObject("blocks").addProperty(key, "minecraft:oak_planks");
                }
            }
        }
    }
    private static @NotNull OllamaChatRequestModel getOllamaChatRequestModel(String description) {
        OllamaChatMessage systemMessage = new OllamaChatMessage(
                OllamaChatMessageRole.SYSTEM, PromptUtils.SYSTEM_PROMPT
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
        // First try to find complete JSON object
        int jsonStart = rawResponse.indexOf('{');
        int jsonEnd = rawResponse.lastIndexOf('}');

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            String potentialJson = rawResponse.substring(jsonStart, jsonEnd + 1);

            // Remove inline comments
            potentialJson = potentialJson.replaceAll("//.*", "");

            try {
                // Validate it's proper JSON
                gson.fromJson(potentialJson, JsonObject.class);
                return potentialJson;
            } catch (Exception e) {
                // Continue to other methods if this fails
            }
        }

        // Fallback to original methods
        if (rawResponse.contains("```json")) {
            int start = rawResponse.indexOf("```json") + "```json".length();
            int end = rawResponse.lastIndexOf("```");
            if (start >= 0 && end > start) {
                return rawResponse.substring(start, end).trim();
            }
        }

        return rawResponse.trim();
    }

    private static Clipboard buildClipboard(JsonObject design) throws MaxChangedBlocksException {
        // 1. Obtener dimensiones
        if (!design.has("dimensions")) {
            throw new IllegalArgumentException("El diseño no contiene dimensiones");
        }
        JsonObject dims = design.getAsJsonObject("dimensions");
        int width = dims.get("width").getAsInt();
        int height = dims.get("height").getAsInt();
        int length = dims.get("length").getAsInt();

        // 2. Crear región
        BlockVector3 min = BlockVector3.at(0, 0, 0);
        BlockVector3 max = BlockVector3.at(
                Math.max(0, width-1),  // Asegurar que no sea negativo
                Math.max(0, height-1),
                Math.max(0, length-1)
        );
        CuboidRegion region = new CuboidRegion(min, max);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        // 3. Configurar bloques DIRECTAMENTE en el clipboard (sin EditSession)
        JsonObject blocks = design.getAsJsonObject("blocks");
        // Debug: Print all blocks from JSON
        System.out.println("=== BLOCKS FROM JSON ===");
        blocks.entrySet().forEach(entry ->
                System.out.println(entry.getKey() + ": " + entry.getValue()));

        for (Map.Entry<String, JsonElement> entry : blocks.entrySet()) {
            try {
                String[] coords = entry.getKey().split(",");
                int x = Integer.parseInt(coords[0].trim());
                int y = Integer.parseInt(coords[1].trim());
                int z = Integer.parseInt(coords[2].trim());

                // Validar coordenadas
                if (x >= width || y >= height || z >= length) {
                    System.err.printf("Coordenada (%d,%d,%d) excede dimensiones (%d,%d,%d)%n",
                            x, y, z, width, height, length);
                    continue;
                }

                String blockId = entry.getValue().getAsString();
                BlockState block = parseBlock(blockId);

                // DEBUG: Verificar bloque antes de colocarlo
                System.out.printf("Colocando %s en (%d,%d,%d)%n", blockId, x, y, z);

                // Colocar directamente en el clipboard
                clipboard.setBlock(BlockVector3.at(x, y, z), block);

            } catch (Exception e) {
                System.err.println("Error procesando bloque " + entry.getKey() + ": " + e.getMessage());
            }
        }

        return clipboard;

    }

    private static BlockState parseBlock(String blockId) {
        try {
            // Asegurar que el ID incluya el namespace
            if (!blockId.startsWith("minecraft:")) {
                blockId = "minecraft:" + blockId;
            }

            // Eliminar propiedades adicionales
            String baseBlockId = blockId.split("\\[")[0].trim();
            BlockType blockType = BlockTypes.get(baseBlockId);

            if (blockType == null) {
                System.err.println("Bloque no encontrado: " + baseBlockId);
                return BlockTypes.STONE.getDefaultState(); // Fallback
            }

            return blockType.getDefaultState();
        } catch (Exception e) {
            System.err.println("Error al parsear bloque: " + blockId);
            return BlockTypes.STONE.getDefaultState();
        }
    }

}