package com.minecraftmods.mcia.util;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ChatUtil {
    public static void sendChatMessage(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message), false);
    }

    public static void sendActionBarMessage(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message), true);
    }
}
