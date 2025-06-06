package com.minecraftmods.mcia.schematics;

public class SchematicUtils {
    public static String getSchematicPath(String type, String material, String style) {
        return String.format("/schematics/%s/%s_%s.schem", type, material, style);
    }

    public static boolean schematicExists(String path) {
        // Verificar si el esquema existe
        return true;
    }
}
