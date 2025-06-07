package com.minecraftmods.mcia.commands;

import com.minecraftmods.mcia.schematics.SchematicLoader;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;

public class SchematicsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("schematics")
                        .then(literal("list")
                                .executes(ctx -> {
                                    SchematicLoader.listSchematics(ctx.getSource());
                                    return 1;
                                })
                        )
        );
    }
}