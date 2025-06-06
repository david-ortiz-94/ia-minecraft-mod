package com.minecraftmods.mcia.commands;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class CommandUtils {
    public static void sendSuccess(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal("✓ " + message).formatted(), false);
    }

    public static void sendError(ServerCommandSource source, String message) {
        source.sendError(Text.literal("✗ " + message));
    }

    public static void sendInfo(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal("ℹ " + message), false);
    }
}
