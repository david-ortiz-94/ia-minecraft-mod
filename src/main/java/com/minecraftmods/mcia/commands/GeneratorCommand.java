package com.minecraftmods.mcia.commands;

import com.minecraftmods.mcia.generation.builders.StructureBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class GeneratorCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("generar")
                        .then(argument("description", StringArgumentType.greedyString())
                                .executes(GeneratorCommand::generateStructure)
                        )
                        .then(literal("generar-test")
                                .executes(GeneratorCommand::generateTestStructure)
                        )
        );
    }

    private static int generateStructure(CommandContext<ServerCommandSource> context) {
        try {
            StructureBuilder.build(context);
            return 1;
        } catch (Exception e) {
            CommandUtils.sendError(context.getSource(), "Error al generar estructura: " + e.getMessage());
            return 0;
        }
    }

    private static int generateTestStructure(CommandContext<ServerCommandSource> context) {
        return StructureBuilder.buildTestStructure(context.getSource());
    }
}