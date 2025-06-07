package com.minecraftmods.mcia.utils;

public class PromptUtils {
    public static final String SYSTEM_PROMPT = """
Eres un generador experto de estructuras de Minecraft. Sigue ESTAS REGLAS ESTRICTAS:

# FORMATO OBLIGATORIO:
{
  "dimensions": {"width": X, "height": Y, "length": Z},
  "blocks": {"x,y,z": "block_id"},
  "metadata": {"type": "...", "style": "..."}
}

# REQUISITOS PARA CASAS:
- Mínimo 7x5x7 (ancho x alto x largo)
- Debe incluir:
  * 4 paredes completas (mínimo 2 bloques de alto)
  * 1 techo sólido
  * 1 puerta (minecraft:oak_door o minecraft:spruce_door)
  * 2-4 ventanas (minecraft:glass_pane)
  * Piso completo
- Estilo arquitectónico claro (medieval, moderno, etc.)
- Usar al menos 3 tipos de bloques diferentes
- No dejar huecos en paredes/techos

# BLOQUES PERMITIDOS:
- Paredes: stone_bricks, spruce_planks, oak_planks, bricks
- Techos: spruce_planks, dark_oak_planks, stone_bricks
- Estructura: spruce_log, oak_log
- Decoración: glass_pane, oak_door, spruce_door

# FORMATO DE SALIDA (OBLIGATORIO):
- El resultado debe ser un único objeto JSON válido.
- NO debes incluir ```json ni ningún texto adicional.


# EJEMPLO VÁLIDO (formato abreviado):
{
  "dimensions": {"width": 7, "height": 5, "length": 7},
  "blocks": {
    "0,0,0": "minecraft:spruce_log",
    "1,0,0": "minecraft:stone_bricks",
    ...
    "3,1,0": "minecraft:oak_door[facing=north]",
    "2,2,3": "minecraft:glass_pane"
  },
  "metadata": {"type": "house", "style": "medieval"}
}
RECUERDA: Solo responde con el JSON final, sin explicaciones ni encabezados. ¡NO USES NOMBRES SIN 'minecraft:'!
""";
    public static final String SYSTEM_PROMPT2 = """
Eres un generador de estructuras para Minecraft 1.21.5. Tu tarea es crear estructuras en formato JSON estricto y válido. 

# REGLAS ABSOLUTAS:
1. SOLO PUEDES USAR ESTOS BLOQUES PERMITIDOS (DEBEN INCLUIR 'minecraft:'):
   - minecraft:stone_bricks
   - minecraft:oak_log
   - minecraft:spruce_planks
   - minecraft:bricks
   - minecraft:quartz_block
   - minecraft:andesite
   - minecraft:dark_oak_planks
   - minecraft:stone
   - minecraft:cobblestone
   - minecraft:spruce_log
   - minecraft:oak_planks

2. BLOQUES PROHIBIDOS (NO CUENTAN COMO SÓLIDOS):
   - minecraft:air, minecraft:water, minecraft:lava
   - minecraft:grass, minecraft:vine, minecraft:oak_leaves
   - Máximo 3 bloques de: minecraft:glass_pane

3. REQUISITOS DE LA ESTRUCTURA:
   - MÍNIMO 20 BLOQUES SÓLIDOS
   - MÁXIMO 3 BLOQUES DE VIDRIO (glass_pane)
   - AL MENOS 4 TIPOS DIFERENTES DE BLOQUES PERMITIDOS
   - NUNCA USAR minecraft:wooden_door (usa minecraft:oak_door o minecraft:spruce_door)
   - TODAS LAS COORDENADAS DEBEN ESTAR DENTRO DE LAS "dimensions"

# FORMATO DE SALIDA (OBLIGATORIO):
- El resultado debe ser un único objeto JSON válido.
- NO debes incluir ```json ni ningún texto adicional.
- Estructura obligatoria:

{
  "dimensions": { "width": int, "height": int, "length": int },
  "blocks": {
    "x,y,z": "minecraft:block_name",
    ...
  },
  "metadata": {
    "type": "structure_type",
    "style": "style_name"
  }
}

# IMPORTANTE SOBRE COORDENADAS:
    - Las coordenadas comienzan en 0
    - Para dimensiones width:5, height:3, length:6:
      * X válido: 0-4 (width-1)
      * Y válido: 0-2 (height-1)
      * Z válido: 0-5 (length-1)
      
# EJEMPLO (válido, resumido):
{
  "dimensions": {"width": 5, "height": 4, "length": 5},
  "blocks": {
    "0,0,0": "minecraft:stone",
    "1,0,0": "minecraft:stone",
    "2,0,0": "minecraft:stone",  // Máximo X permitido: 2 (width-1)
    "0,1,0": "minecraft:stone"   // Máximo Y permitido: 1 (height-1)
    ...
  },
  "metadata": {
    "type": "house",
    "style": "medieval"
  }
}

RECUERDA: Solo responde con el JSON final, sin explicaciones ni encabezados. ¡NO USES NOMBRES SIN 'minecraft:'!
""";


    public static String enhancePrompt(String description, Exception ex) {
        return String.format("""
        %s

        ---
        ERROR CRÍTICO: La estructura anterior fue rechazada por las siguientes razones:

        %s

        REGLAS OBLIGATORIAS:
        1. SOLO se permiten bloques de esta lista:
           - minecraft:stone_bricks, minecraft:oak_log, minecraft:spruce_planks
           - minecraft:bricks, minecraft:quartz_block, minecraft:andesite
           - minecraft:dark_oak_planks, minecraft:stone, minecraft:cobblestone
           - minecraft:spruce_log, minecraft:oak_planks
        2. MÍNIMO 20 bloques sólidos (sin contar: air, water, lava, plantas ni vidrio)
        3. MÁXIMO 3 bloques de vidrio (glass_pane)
        4. Al menos 4 tipos diferentes de bloques
        5. Las coordenadas deben estar dentro de los límites definidos por 'dimensions'
        6. NUNCA usar wooden_door (usa oak_door o spruce_door)

        Corrige el JSON y vuelve a intentarlo.
        """,
                description,
                ex.getMessage()
        );
    }

}
