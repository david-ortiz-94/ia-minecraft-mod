//package com.minecraftmods.mcia.schematics;
//
//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;
//
//public class ChunkedStructureBuilder {
//    private static final int MAX_CHUNK_SIZE = 16; // Bloques por chunk (16x16x16)
//    private static final int MAX_TOKENS = 3072; // Límite de tokens para Llama3.2
//
//    public static JsonObject generateLargeStructure(String description){
//        // 1. Primera consulta para el diseño general
//        JsonObject structureBlueprint = getStructureBlueprint(description);
//
//        // 2. Dividir en chunks
//        JsonArray chunks = structureBlueprint.getAsJsonArray("chunks");
//        JsonObject fullStructure = new JsonObject();
//        JsonObject allBlocks = new JsonObject();
//
//        // 3. Procesar cada chunk en paralelo
//        chunks.parallelStream().forEach(chunk -> {
//            JsonObject chunkData = processChunk(chunk.getAsJsonObject());
//            chunkData.getAsJsonObject("blocks").entrySet().forEach(entry -> {
//                allBlocks.add(entry.getKey(), entry.getValue());
//            });
//        });
//
//        fullStructure.add("blocks", allBlocks);
//        fullStructure.add("dimensions", structureBlueprint.get("dimensions"));
//        return fullStructure;
//    }
//
//    private static JsonObject getStructureBlueprint(String description) {
//        String prompt = """
//            Eres un generador de estructuras para Minecraft. Para la descripción '%s':
//            1. Divide la estructura en chunks de máximo %dx%dx%d bloques
//            2. Genera un plano general con las dimensiones totales
//            3. Proporciona descripciones detalladas para cada chunk
//
//            Respuesta en JSON con: {
//                "dimensions": {"width": W, "height": H, "length": L},
//                "chunks": [
//                    {
//                        "x": X, "y": Y, "z": Z,
//                        "size": S,
//                        "description": "Descripción detallada del chunk"
//                    }
//                ]
//            }
//            """.formatted(description, MAX_CHUNK_SIZE, MAX_CHUNK_SIZE, MAX_CHUNK_SIZE);
//
//        return LlamaInterface.query(prompt);
//    }
//
//    private static JsonObject processChunk(JsonObject chunk) {
//        String prompt = """
//            Genera el chunk en Minecraft con estas características:
//            - Posición: X=%d Y=%d Z=%d
//            - Tamaño: %d bloques
//            - Descripción: %s
//
//            Devuelve EXACTAMENTE este JSON:
//            {
//                "blocks": {
//                    "x,y,z": "block_id",
//                    "0,0,0": "minecraft:stone"
//                }
//            }
//            """.formatted(
//                chunk.get("x").getAsInt(),
//                chunk.get("y").getAsInt(),
//                chunk.get("z").getAsInt(),
//                chunk.get("size").getAsInt(),
//                chunk.get("description").getAsString()
//        );
//
//        return LlamaInterface.query(prompt);
//    }
//}