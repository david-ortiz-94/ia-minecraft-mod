package com.minecraftmods.mcia.nlp;

public class PromptBuilder {
    public static String buildIntentPrompt() {
        return """
                Eres un clasificador de intenciones para un mod de Minecraft. Clasifica la siguiente petición del jugador en una de estas categorías:
                - STRUCTURE: Cuando pida generar estructuras como casas, castillos, etc.
                - ITEM: Cuando pida generar objetos como armas, herramientas, etc.
                - DECORATION: Cuando pida generar elementos decorativos.
                
                Responde únicamente con una de las palabras en mayúsculas indicadas arriba.
                """;
    }

    public static String buildStructurePrompt2() {
        return """
                Eres un asistente para generación de estructuras en Minecraft. El jugador te dará una descripción y debes extraer:
                1. Tipo de estructura (casa, castillo, puente, etc.)
                2. Material principal (madera, piedra, ladrillo, etc.)
                3. Estilo (medieval, moderno, futurista, etc.)
                4. Tamaño (pequeño, mediano, grande)
                
                Devuelve la respuesta en formato JSON con esas claves.
                """;
    }

    public static String buildStructurePrompt() {
        return """
        Eres un generador especializado en estructuras para Minecraft 1.21.5. 
        Convierte descripciones en parámetros técnicos para .schem (WorldEdit).
        
        REQUISITOS:
        1. Bloques disponibles: Usa solo bloques de Minecraft 1.21.5 (ej: 'minecraft:stone_bricks', no 'stone bricks')
        2. Dimensiones: Máximo 64x64x64 (límite de WorldEdit)
        3. Formatos: Solo JSON válido (sin markdown o comentarios)
        
        DEVUELVE EXACTAMENTE este JSON:
        {
          "type": "[tipo]",
          "blocks": {
            "primary": "minecraft:[bloque_principal]",
            "secondary": "minecraft:[bloque_secundario]",
            "decorations": ["minecraft:[bloque1]", "minecraft:[bloque2]"]
          },
          "dimensions": {
            "width": [ancho],
            "height": [alto],
            "length": [largo]
          },
          "style": "[estilo]",
          "features": ["[característica1]", "[característica2]"]
        }
        
        EJEMPLOS:
        
        Input: "casa medieval de piedra con techo de madera oscura"
        Output:
        {
          "type": "house",
          "blocks": {
            "primary": "minecraft:stone_bricks",
            "secondary": "minecraft:dark_oak_planks",
            "decorations": ["minecraft:spruce_fence", "minecraft:lantern"]
          },
          "dimensions": {
            "width": 12,
            "height": 8,
            "length": 10
          },
          "style": "medieval",
          "features": ["chimenea", "ventanas arqueadas"]
        }
        
        Input: "torre futurista de cristal pequeña"
        Output:
        {
          "type": "tower",
          "blocks": {
            "primary": "minecraft:glass",
            "secondary": "minecraft:white_concrete",
            "decorations": ["minecraft:light_blue_stained_glass", "minecraft:end_rod"]
          },
          "dimensions": {
            "width": 7,
            "height": 15,
            "length": 7
          },
          "style": "futurist",
          "features": ["iluminación LED", "paneles transparentes"]
        }
        
        REGLAS:
        - Bloques secundarios/decorativos: Máximo 3 tipos
        - Tamaños proporcionales (ancho ≈ largo)
        - Estilos permitidos: medieval, modern, futurist, rustic, fantasy
        - Si no hay detalles suficientes, usa valores por defecto realistas
        """;
    }
}