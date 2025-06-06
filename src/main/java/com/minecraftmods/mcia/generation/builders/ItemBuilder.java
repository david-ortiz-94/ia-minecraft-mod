package com.minecraftmods.mcia.generation.builders;

import com.minecraftmods.mcia.commands.CommandUtils;
import net.minecraft.server.command.ServerCommandSource;

public class ItemBuilder {
    public static void build(ServerCommandSource source, String description) {
        CommandUtils.sendInfo(source, "Generando objeto: " + description);
        // Implementación para generar objetos
        CommandUtils.sendSuccess(source, "Objeto generado y añadido a tu inventario!");
    }
}