package com.minecraftmods.mcia.schematics;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Schematic {
    private final String[][][] blocks; // [x][y][z]
    private final int width, height, length;

    public Schematic(int width, int height, int length) {
        this.width = Math.min(width, 64); // Limitar a máximo de WorldEdit
        this.height = Math.min(height, 64);
        this.length = Math.min(length, 64);
        this.blocks = new String[width][height][length];
    }

    public void fillFoundation(String blockId) {
        // Llena paredes y piso
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                blocks[x][0][z] = blockId; // Piso
                if (x == 0 || x == width-1 || z == 0 || z == length-1) {
                    for (int y = 1; y < height; y++) {
                        blocks[x][y][z] = blockId; // Paredes
                    }
                }
            }
        }
    }

    public void exportToFile(String path) throws IOException {
        // Implementación real usaría la librería de schematics de WorldEdit
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(path))) {
            // Escribir formato .schem
            out.writeShort(width);
            out.writeShort(height);
            out.writeShort(length);

            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        String block = blocks[x][y][z] != null ? blocks[x][y][z] : "minecraft:air";
                        out.writeUTF(block);
                    }
                }
            }
        }
    }
}
