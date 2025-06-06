package com.minecraftmods.mcia.test;


import com.minecraftmods.mcia.commands.GeneratorCommand;
import com.minecraftmods.mcia.generation.builders.StructureBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TestMod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MCIA-Test");

    @Override
    public void onInitialize() {
        LOGGER.info("[TestMod] Registrando prueba automática de comando /generar");

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("[TestMod] Ejecutando prueba de generación...");

            try {
                ServerWorld world = server.getOverworld();
                BlockPos pos = new BlockPos(0, 64, 0);

                ServerCommandSource fakeSource = new ServerCommandSource(
                        server,
                        Vec3d.ofCenter(pos),
                        Vec2f.ZERO,
                        world,
                        4,
                        "TestSource",
                        Text.literal("TestSource"),
                        server,
                        null
                );
                // Get the dispatcher from the server
                CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();

// Replace "generar" with your actual command name
                // Create a dummy command node
                LiteralCommandNode<ServerCommandSource> generarNode  = LiteralArgumentBuilder
                        .<ServerCommandSource>literal("generar")
                        .then(RequiredArgumentBuilder.argument("description", StringArgumentType.string()))
                        .executes(context -> 0).build();
                // 3. Prepare the description
                String description = "una casa japonesa moderna";
                // Calculate positions (assuming the argument starts after "generar ")
                int start = "generar ".length(); // Start after command prefix
                int end = start + description.length();

                // 4. Create the parsed argument (using YOUR constructor)
                ParsedArgument<ServerCommandSource, String> parsedArg = new ParsedArgument<>(
                        start,          // int start position
                        end,            // int end position
                        description      // T result (String)
                );

                // 5. Build the context
                CommandContextBuilder<ServerCommandSource> contextBuilder = new CommandContextBuilder<>(
                        dispatcher,
                        fakeSource,
                        generarNode,
                        0
                );
                contextBuilder.withArgument("description", parsedArg);
                LOGGER.info("[TestMod] Generando estructura: una casa japonesa");
                // 6. Build with raw command input
                String commandInput = "generar " + description;
                CommandContext<ServerCommandSource> fakeContext = contextBuilder.build(commandInput);

                // 7. Execute test
                LOGGER.info("[TestMod] Generando estructura...");
                StructureBuilder.build(fakeContext);

            } catch (Exception e) {
                LOGGER.error("[TestMod] Error en prueba de comando /generar", e);
            }
        });
    }
}
