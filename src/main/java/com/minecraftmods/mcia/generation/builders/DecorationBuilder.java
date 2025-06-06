package com.minecraftmods.mcia.generation.builders;

import com.minecraftmods.mcia.commands.CommandUtils;
import net.minecraft.server.command.ServerCommandSource;

public class DecorationBuilder {
    public static void build(ServerCommandSource source, String description) {
        CommandUtils.sendInfo(source, "Generando decoración: " + description);
        // Implementación para generar decoraciones
        CommandUtils.sendSuccess(source, "Decoración generada con éxito!");
    }
}
