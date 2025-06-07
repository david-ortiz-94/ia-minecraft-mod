package com.minecraftmods.mcia.generation.builders;


import com.minecraftmods.mcia.commands.CommandUtils;
import com.minecraftmods.mcia.nlp.OllamaStructureGenerator;
import com.minecraftmods.mcia.schematics.SchematicLoader;
import com.minecraftmods.mcia.schematics.SchematicPaster;
import com.minecraftmods.mcia.utils.BlockUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static com.minecraftmods.mcia.utils.PromptUtils.enhancePrompt;


public class StructureBuilder {
    private static final String GENERATED_TYPE = "generated";
    private static final String CUSTOM_MATERIAL = "custom";
    private static final String AI_STYLE = "ai";


    public static void build(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String description = StringArgumentType.getString(context, "description");

        OllamaStructureGenerator.generateStructureAsync(description)
                .handleAsync((clipboard, e) -> {
                    if (e != null) throw new CompletionException(e);
                    try {
                        validateClipboard(clipboard);
                        return clipboard;
                    } catch (IOException ex) {
                        return retryGeneration(description, 2, ex);
                    }
                })
                .thenAcceptAsync(clipboard -> {
                    try {

                        Path schematicPath = SchematicLoader.saveGeneratedSchematic(clipboard, GENERATED_TYPE, CUSTOM_MATERIAL, AI_STYLE);
                        BlockPos pos = BlockPos.ofFloored(source.getPosition());
                        try (EditSession editSession = WorldEdit.getInstance()
                                .newEditSessionBuilder()
                                .world(FabricAdapter.adapt(source.getWorld()))
                                .build()) {

                            ClipboardHolder holder = new ClipboardHolder(clipboard);
                            Operation operation = holder.createPaste(editSession)
                                    .to(BlockVector3.at(pos.getX(), pos.getY(), pos.getZ()))
                                    .ignoreAirBlocks(false)
                                    .build();

                            Operations.complete(operation);
                        }

                        SchematicPaster.pasteSchematic(source, pos, schematicPath);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, source.getServer())
                .exceptionally(e -> {
                    Throwable cause = e.getCause();
                    if (cause instanceof HttpTimeoutException) {
                        source.sendError(Text.literal("⌛ El servidor Ollama no respondió (timeout)"));
                    } else {
                        handleGenerationError(source, e);
                    }
                    return null;
                });
    }

    public static int buildTestStructure(ServerCommandSource source) {
        BlockPos playerPos = source.getPlayer().getBlockPos();
        try {
            SchematicPaster.pasteSchematic(source, playerPos,
                    SchematicLoader.getSchematicPath("test", "stone", "basic"));
            CommandUtils.sendSuccess(source, "Estructura de prueba generada con éxito!");
            return 1;
        } catch (Exception e) {
            CommandUtils.sendError(source, "Error al generar estructura de prueba: " + e.getMessage());
            return 0;
        }
    }

    private static Clipboard retryGeneration(String description, int attempts, Exception originalEx) {
        if (attempts <= 0) {
            throw new CompletionException(originalEx);
        }

        try {
            // Añadir feedback al prompt
            String enhancedPrompt = enhancePrompt(description, originalEx);

            Clipboard clipboard = OllamaStructureGenerator.generateStructureAsync(enhancedPrompt).join();
            validateClipboard(clipboard);
            return clipboard;
        } catch (Exception e) {
            return retryGeneration(description, attempts - 1, e);
        }
    }

    private static void validateClipboard(Clipboard clipboard) throws IOException {
        System.out.println("=== VALIDATION DEBUG ===");

        int solidBlocks = 0;
        Set<String> validBlocksFound = new HashSet<>();

        for (BlockVector3 pos : clipboard.getRegion()) {
            BlockState block = clipboard.getBlock(pos);
            String blockId = block.getBlockType().id();
            boolean isSolid = BlockUtils.isSolid(block);

            //System.out.printf("Block at %s: %s -> %s%n", pos, blockId, isSolid ? "VALID" : "INVALID");

            if (isSolid) {
                solidBlocks++;
                validBlocksFound.add(blockId);
            }
        }

        if (solidBlocks < 10) {
            throw new IOException(String.format(
                    "Insufficient structure: %d solid blocks. Valid blocks found: %s",
                    solidBlocks, String.join(", ", validBlocksFound)
            ));
        }
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

}