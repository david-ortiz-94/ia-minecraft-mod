package com.minecraftmods.mcia.schematics;

import com.minecraftmods.mcia.utils.FeedbackUtils;
import com.minecraftmods.mcia.utils.PositionUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Path;

public class SchematicPaster {
    public static void pasteSchematic(ServerCommandSource source, BlockPos position, Path schematicPath) throws IOException, WorldEditException {
        Clipboard clipboard = SchematicValidator.loadAndValidate(schematicPath);
        World world = FabricAdapter.adapt(source.getWorld());

        BlockVector3 pastePos = PositionUtils.calculatePastePosition(world, position, clipboard);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(FabricAdapter.adapt(source.getWorld()))) {
            new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(pastePos)
                    .ignoreAirBlocks(false)
                    .build();

            editSession.close();
            FeedbackUtils.sendSuccessFeedback(source, pastePos, clipboard);
        }
    }
}