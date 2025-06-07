package com.minecraftmods.mcia.schematics;

import com.minecraftmods.mcia.utils.BlockUtils;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SchematicValidator {
    public static Clipboard loadAndValidate(Path schematicPath) throws IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(schematicPath.toFile());
        if (format == null) {
            throw new IOException("Formato de esquema no reconocido");
        }

        try (ClipboardReader reader = format.getReader(Files.newInputStream(schematicPath))) {
            Clipboard clipboard = reader.read();
            if (!isValid(clipboard)) {
                throw new IOException("El esquema no contiene bloques sólidos válidos");
            }
            return clipboard;
        }
    }

    public static boolean isValid(Clipboard clipboard) {
        Region region = clipboard.getRegion();
        for (BlockVector3 pos : region) {
            if (BlockUtils.isSolid(clipboard.getBlock(pos))) {
                return true;
            }
        }
        return false;
    }
}