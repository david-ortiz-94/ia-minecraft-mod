package com.minecraftmods.mcia.schematics;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Random;

public class SchematicLoader {
    public static final String SCHEMATICS_DIR = "config/mcia/schematics";

    public static Path saveGeneratedSchematic(Clipboard clipboard, String type, String material, String style) throws IOException {
        System.out.println("INICIO DEL METODO DE GUARDADO saveGeneratedSchematic");
        Path tempFile = saveTemporarySchematic(clipboard);
        System.out.println("GUARDADO TEMP: " + tempFile.toAbsolutePath().toString());
        return moveToSchematicDir(tempFile, type, material, style);
    }

    public static Path getSchematicPath(String type, String material, String style) throws IOException {
        String filename = String.format("%s_%s_%s.schem", type, material, style);
        Path path = Paths.get(SCHEMATICS_DIR, filename);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("Esquema no encontrado: " + filename);
        }

        return path;
    }

    public static void listSchematics(ServerCommandSource source) {
        File schematicsDir = new File(SCHEMATICS_DIR);
        if (!schematicsDir.exists()) {
            source.sendMessage(Text.literal("Directorio no existe: " + schematicsDir.getAbsolutePath()));
            return;
        }

        File[] files = schematicsDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".schem") ||
                        name.toLowerCase().endsWith(".schematic"));

        if (files == null || files.length == 0) {
            source.sendMessage(Text.literal("No se encontraron esquemas"));
            return;
        }

        source.sendMessage(Text.literal("Esquemas disponibles (" + files.length + "):"));
        Arrays.stream(files)
                .sorted()
                .forEach(file -> source.sendMessage(Text.literal("- " + file.getName())));
    }

    private static Path saveTemporarySchematic(Clipboard clipboard) throws IOException {
        Path tempFile = Files.createTempFile("mc_ai_", ".schem");
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(
                new FileOutputStream(tempFile.toFile()))) {
            writer.write(clipboard);
        }
        return tempFile;
    }

    private static Path moveToSchematicDir(Path tempSchem, String type, String material, String style)
            throws IOException {
        String filename = String.format("%s_%s_%s_%s.schem", type, material, style, new Random().nextInt());
        Path schemDir = Path.of(SCHEMATICS_DIR);

        if (!Files.exists(schemDir)) {
            Files.createDirectories(schemDir);
        }

        Path finalPath = schemDir.resolve(filename);
        Files.move(tempSchem, finalPath, StandardCopyOption.REPLACE_EXISTING);
        return finalPath;
    }
}