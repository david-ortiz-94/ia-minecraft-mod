package com.minecraftmods.mcia.generation;

import com.minecraftmods.mcia.commands.CommandUtils;
import com.minecraftmods.mcia.generation.builders.DecorationBuilder;
import com.minecraftmods.mcia.generation.builders.ItemBuilder;
import com.minecraftmods.mcia.generation.builders.StructureBuilder;
import com.minecraftmods.mcia.nlp.NLPProcessor;
import net.minecraft.server.command.ServerCommandSource;

public class GenerationEngine {
    public static int generate(ServerCommandSource source, String description) {
        NLPProcessor.Intent intent = NLPProcessor.getIntention(description);

        switch (intent) {
//            case STRUCTURE:
//                StructureBuilder.build(source, description);
//                break;
            case ITEM:
                ItemBuilder.build(source, description);
                break;
            case DECORATION:
                DecorationBuilder.build(source, description);
                break;
            default:
                CommandUtils.sendError(source, "No pude entender lo que quieres generar.");
                return 0;
        }

        return 1;
    }
}
