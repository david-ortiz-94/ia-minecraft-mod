package com.minecraftmods.mcia.generation.builders;


import com.google.gson.JsonObject;
import com.minecraftmods.mcia.nlp.OllamaStructureGenerator;
import com.minecraftmods.mcia.schematics.SchematicManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static com.minecraftmods.mcia.schematics.SchematicManager.NON_SOLID_BLOCKS;

public class StructureBuilder {
    private static final int SEARCH_RADIUS = 20; // Bloques a buscar alrededor del jugador
    private static OllamaStructureGenerator generator ;

    public StructureBuilder(OllamaStructureGenerator generator) {
        StructureBuilder.generator = generator;
    }

    private static Path saveTemporarySchemati2(Clipboard clipboard) throws IOException {
        Path tempFile = Files.createTempFile("mc_ai_", ".schem");
        System.out.println("[DEBUG] Schematic guardado en: " + tempFile.toAbsolutePath());
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(
                new FileOutputStream(tempFile.toFile()))) {
            writer.write(clipboard);
        }
        return tempFile;
    }

    private static void pasteSchematic(ServerCommandSource source, Path schemPath) {
        try {
            BlockPos pos = BlockPos.ofFloored(source.getPosition());
            String fileName = schemPath.getFileName().toString().replace(".schem", "");

            // 1. First copy to WorldEdit's schematics folder
            Path weSchemDir = Path.of("config/worldedit/schematics");
            if (!Files.exists(weSchemDir)) {
                Files.createDirectories(weSchemDir);
            }
            Path dest = weSchemDir.resolve(schemPath.getFileName());
            Files.copy(schemPath, dest, StandardCopyOption.REPLACE_EXISTING);

            // 2. Execute commands separately with proper delays
            String loadCmd = String.format("//schem load %s", fileName);
           // String pasteCmd = String.format("//paste -a -o %d %d %d",pos.getX(), pos.getY(), pos.getZ());
            String pasteCmd = "//paste -a";

            // Execute commands with delay between them
            source.getServer().getCommandManager().executeWithPrefix(source, loadCmd);
            Thread.sleep(500); // Wait for load to complete
            source.getServer().getCommandManager().executeWithPrefix(source, pasteCmd);

        } catch (Exception e) {
            source.sendError(Text.literal("✖ Error al pegar: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private static void pasteSchematic2(ServerCommandSource source, Path schemPath) {
        String cmd = String.format(
                "schematics load %s && paste at %f %f %f",
                schemPath.getFileName(),
                source.getPosition().getX(),
                source.getPosition().getY(),
                source.getPosition().getZ()
        );

        source.getServer().getCommandManager().executeWithPrefix(source, cmd);
    }
    public static void build(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String description = StringArgumentType.getString(context, "description");

        OllamaStructureGenerator.generateStructureAsync(description)
                .handleAsync((clipboard, e) -> {
                    if (e != null) {
                        throw new CompletionException(e);
                    }
                    try {
                        validateClipboard(clipboard);
                        return clipboard;
                    } catch (IOException ex) {
                        // Reintentar hasta 2 veces si la estructura es inválida
                        return retryGeneration(description, 2, ex);
                    }
                })
                .thenAcceptAsync(clipboard -> {
                    try {
                        validateClipboard(clipboard); // ← Nueva validación

                        // 1. Guardar el schematic temporal
                        Path tempSchem = saveTemporarySchematic(clipboard);

                        // 2. Mover al directorio de schematics permanente
                        Path finalPath = moveToSchematicDir(tempSchem, "generated", "custom", "ai");
                        System.out.println("[DEBUG] Schematic guardado en: " + finalPath.toAbsolutePath());

                        // 3. Generar la estructura usando el SchematicManager
                        BlockPos pos = BlockPos.ofFloored(source.getPosition());
                        boolean success = SchematicManager.generateStructure(
                                source,
                                pos,
                                "generated",  // type
                                "custom",    // material
                                "ai"         // style
                        );

                        if (!success) {
                            throw new IOException("No se pudo generar la estructura");
                        }

                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, context.getSource().getServer())
                .exceptionally(e -> {
                    handleGenerationError(source, e);
                    return null;
                });
    }
    private static Clipboard retryGeneration(String description, int attempts, Exception originalEx) {
        if (attempts <= 0) {
            throw new CompletionException(originalEx);
        }

        try {
            // Añadir feedback al prompt
            String enhancedPrompt = String.format(
                    "%s\nNOTA: En intentos anteriores hubo estos problemas:\n" +
                            "1. Usaste bloques inválidos como wooden_door (usa oak_door)\n" +
                            "2. Coordenadas fuera del rango dimensional\n" +
                            "3. Estructuras con muy pocos bloques sólidos\n" +
                            "Por favor corrige estos problemas en tu respuesta.",
                    description
            );

            Clipboard clipboard = OllamaStructureGenerator.generateStructureAsync(enhancedPrompt).join();
            validateClipboard(clipboard);
            return clipboard;
        } catch (Exception e) {
            return retryGeneration(description, attempts - 1, e);
        }
    }
    private static void validateClipboard(Clipboard clipboard) throws IOException {
        int solidBlocks = 0;
        Set<BlockType> usedBlockTypes = new HashSet<>();

        for (BlockVector3 pos : clipboard.getRegion()) {
            BlockType blockType = clipboard.getBlock(pos).getBlockType();
            if (!NON_SOLID_BLOCKS.contains(blockType)) {
                solidBlocks++;
                usedBlockTypes.add(blockType);
            }
        }

        if (solidBlocks < 5) {
            throw new IOException(String.format(
                    "Estructura insuficiente: %d bloques sólidos (%d tipos diferentes)",
                    solidBlocks,
                    usedBlockTypes.size()
            ));
        }
    }
    private static Path saveTemporarySchematic(Clipboard clipboard) throws IOException {
        Path tempFile = Files.createTempFile("mc_ai_", ".schem");
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(
                new FileOutputStream(tempFile.toFile()))) {
            writer.write(clipboard);
        }
        return tempFile;
    }

    private static Path moveToSchematicDir(Path tempSchem, String type, String material, String style)
            throws IOException {
        String filename = String.format("%s_%s_%s.schem", type, material, style);
        Path schemDir = Path.of(SchematicManager.SCHEMATICS_DIR);

        if (!Files.exists(schemDir)) {
            Files.createDirectories(schemDir);
        }

        Path finalPath = schemDir.resolve(filename);
        Files.move(tempSchem, finalPath, StandardCopyOption.REPLACE_EXISTING);
        return finalPath;
    }

    private static void handleGenerationError(ServerCommandSource source, Throwable e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        String errorMessage;

        if (cause instanceof HttpTimeoutException) {
            errorMessage = "⌛ Tiempo de espera agotado al generar la estructura";
        } else if (cause instanceof IOException) {
            errorMessage = "✖ Error de E/S: " + cause.getMessage();
        } else {
            errorMessage = "✖ Error inesperado: " + cause.getMessage();
        }

        source.sendError(Text.literal(errorMessage));
        cause.printStackTrace();
    }
    public static void build3(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String description = StringArgumentType.getString(context, "description");

        OllamaStructureGenerator.generateStructureAsync(description)
                .thenAcceptAsync(clipboard -> {
                    try {
                        Path schemPath = saveTemporarySchematic(clipboard);
                        pasteSchematic(source, schemPath);
                        source.sendFeedback(() ->
                                Text.literal("✅ Estructura generada con éxito!"), false);
                    } catch (Exception e) {
                        source.sendError(Text.literal("✖ Error al construir: " +
                                (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
                        e.printStackTrace();
                    }
                }, context.getSource().getServer())
                .exceptionally(e -> {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;

                    String errorMessage;
                    if (cause instanceof HttpTimeoutException) {
                        errorMessage = "⌛ Tiempo de espera agotado al generar la estructura. Inténtalo de nuevo más tarde.";
                    } else if (cause instanceof CompletionException && cause.getCause() != null) {
                        errorMessage = "✖ Error: " + cause.getCause().getMessage();
                    } else {
                        errorMessage = "✖ Error inesperado: " + cause.getMessage();
                    }

                    source.sendError(Text.literal(errorMessage));
                    cause.printStackTrace();
                    return null;
                });
    }
    public static void build2(CommandContext<ServerCommandSource> context) {
        //try {
            // 1. Procesar parámetros de la descripción
            ServerCommandSource source = context.getSource();
            String description = StringArgumentType.getString(context, "description");

            OllamaStructureGenerator.generateStructureAsync(description)
                    .thenAcceptAsync(clipboard -> {
                        try {
                            // Guardar schematic temporal
                            Path schemPath = saveTemporarySchematic(clipboard);

                            // Cargar y pegar en el mundo
                            pasteSchematic(source, schemPath);

                            source.sendFeedback(()->
                                    Text.literal("✅ Estructura generada con éxito!"),
                                    false
                            );
                        } catch (Exception e) {
                            source.sendError(Text.literal("✖ Error al construir: " + e.getMessage()));
                        }
                    }, context.getSource().getServer())
                    .exceptionally(e -> {
                        source.sendError(Text.literal("✖ Error de generación: " + e.getCause().getMessage()));
                        return null;
                    });
            /*// 2. Extraer parámetros
            JsonObject blocks = params.getAsJsonObject("blocks");
            JsonObject dimensions = params.getAsJsonObject("dimensions");

            String primaryBlock = blocks.get("primary").getAsString();
            String secondaryBlock = blocks.get("secondary").getAsString();
            JsonArray decorations = blocks.getAsJsonArray("decorations");

            // 3. Crear esquema temporal
            Schematic schematic = new Schematic(width, height, length);

            // Construir estructura básica
            schematic.fillFoundation(primaryBlock); // Paredes y piso
            schematic.buildRoof(secondaryBlock); // Techo

            // Añadir decoraciones
            for (JsonElement decor : decorations) {
                schematic.addDecoration(decor.getAsString());
            }

            // 4. Encontrar ubicación adecuada
            BlockPos buildPos = findBestLocation(
                    source.getWorld(),
                    source.getPlayer().getBlockPos()
            );

            // 4. Generar estructura
            boolean success = SchematicManager.generateStructure(
                    source,
                    buildPos,
                    type,
                    material,
                    style
            );

            // 5. Manejar resultado
            if (success) {
                CommandUtils.sendSuccess(source, "¡Estructura generada con éxito en X:%d Y:%d Z:%d!"
                        .formatted(buildPos.getX(), buildPos.getY(), buildPos.getZ()));
            } else {
                CommandUtils.sendError(source, "No se pudo generar la estructura.");
            }
*/
//        } catch (IllegalArgumentException e) {
//            CommandUtils.sendError(source, e.getMessage());
//        } catch (Exception e) {
//            CommandUtils.sendError(source, "Error inesperado al generar estructura.");
//            e.printStackTrace();
//        }
    }

    private static void validateParams(JsonObject params) {
        if (params == null || params.size() == 0) {
            throw new IllegalArgumentException("No se pudieron obtener parámetros válidos de la descripción.");
        }
        if (!params.has("blocks") || !params.has("dimensions")) {
            throw new IllegalArgumentException("Respuesta de IA incompleta");
        }
    }

    /**
     * Encuentra la mejor ubicación para construir cerca del jugador
     */
    private static BlockPos findBestLocation(ServerWorld world, BlockPos center) {
        // 1. Primero intentar en la posición actual del jugador
        BlockPos currentPos = world.getTopPosition(
                Heightmap.Type.WORLD_SURFACE,
                center
        ).down(); // Ajustar a la superficie

        if (isAreaSuitable(world, currentPos)) {
            return currentPos;
        }

        // 2. Buscar en espiral alrededor del jugador
        for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) == radius || Math.abs(z) == radius) {
                        BlockPos testPos = center.add(x, 0, z);
                        BlockPos surfacePos = world.getTopPosition(
                                Heightmap.Type.WORLD_SURFACE,
                                testPos
                        ).down();

                        if (isAreaSuitable(world, surfacePos)) {
                            return surfacePos;
                        }
                    }
                }
            }
        }

        // 3. Si no se encuentra, usar la posición original con ajustes
        return currentPos;
    }

    /**
     * Verifica si un área es adecuada para construcción
     */
    private static boolean isAreaSuitable(ServerWorld world, BlockPos pos) {
        // Verificar que los bloques adyacentes son sólidos
        return world.getBlockState(pos).isSolid()
                && world.getBlockState(pos.up()).isAir()
                && world.getBlockState(pos.north()).isSolid()
                && world.getBlockState(pos.south()).isSolid()
                && world.getBlockState(pos.east()).isSolid()
                && world.getBlockState(pos.west()).isSolid();
    }
}