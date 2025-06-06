package com.minecraftmods.mcia.commands;

import com.minecraftmods.mcia.generation.GenerationEngine;
import com.minecraftmods.mcia.generation.builders.StructureBuilder;
import com.minecraftmods.mcia.schematics.SchematicManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class GeneratorCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        try {

            dispatcher.register(
                    literal("generar")
                            .then(argument("description", StringArgumentType.greedyString())
                                    .executes(GeneratorCommand::generateStructure)
                            )
                            .then(
                                    literal("generar-test")
                                            .executes(GeneratorCommand::generateTestStructure)
                            )
                            .then(
                                    literal("list-schematics")
                                            .executes(ctx -> {
                                                SchematicManager.listSchematics(ctx.getSource());
                                                return 1;
                                            })
                            )
            );

        } catch (Exception e) {
            System.err.println("Error al inicializar comandos: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private static int generateStructure(CommandContext<ServerCommandSource> context) {

        try {
            // Procesar NLP y generar
            //GenerationEngine.generate(context, description);
            StructureBuilder.build(context);
            return 1;
        } catch (Exception e) {

            return 0;
        }
    }

    private static int generateTestStructure(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        BlockPos playerPos = source.getPlayer().getBlockPos();

        try {
            boolean success = SchematicManager.generateStructure(
                    source,
                    playerPos,
                    "test",
                    "stone",
                    "basic"
            );

            if (success) {
                source.sendMessage(Text.literal("Estructura generada con éxito!"));
                return 1;
            } else {
                source.sendMessage(Text.literal("Error al generar la estructura"));
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            source.sendMessage(Text.literal("Error grave: " + e.getMessage()));
            return -1;
        }
    }

    public static void generateStructureWithDescription(ServerCommandSource source, String description) {
        try {
            source.sendMessage(Text.literal("🧪 Ejecutando generación con descripción: " + description));
            GenerationEngine.generate(source, description);

        } catch (Exception e) {
            source.sendError(Text.literal("❌ Error al generar: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}