package com.minecraftmods.mcia.schematics;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SchematicManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("MCIA-SchematicManager");
    public static final String SCHEMATICS_DIR = "config/mcia/schematics";
    public static final Set<BlockType> NON_SOLID_BLOCKS = new HashSet<>(Arrays.asList(
            BlockTypes.AIR,
            BlockTypes.CAVE_AIR,
            BlockTypes.VOID_AIR,
            BlockTypes.WATER,
            BlockTypes.LAVA
//            BlockTypes.GRASS,
//            BlockTypes.TALL_GRASS,
//            BlockTypes.ACACIA_LEAVES,
//            BlockTypes.BIRCH_LEAVES,
//            BlockTypes.OAK_LEAVES,
//            BlockTypes.SPRUCE_LEAVES,
//            // Verificar que estos tipos existan en tu versión:
//            BlockTypes.AZALEA_LEAVES,
//            BlockTypes.CHERRY_LEAVES,
//            BlockTypes.DARK_OAK_LEAVES,
//            BlockTypes.FLOWERING_AZALEA_LEAVES,
//            BlockTypes.JUNGLE_LEAVES,
//            BlockTypes.MANGROVE_LEAVES,
//            BlockTypes.PALE_OAK_LEAVES
    ));

    static {
        NON_SOLID_BLOCKS.removeIf(blockType -> blockType == null);
    }

    public static boolean generateStructure(ServerCommandSource source, BlockPos position,
                                            String type, String material, String style) {
        try {
            // Validación y preparación inicial
            validateSource(source);
            ServerWorld serverWorld = source.getWorld();
            World world = FabricAdapter.adapt(serverWorld);

            // Carga del esquema con validación
            Path schematicPath = getSchematicPath(type, material, style);
            Clipboard clipboard = loadAndValidateSchematic(schematicPath);

            // Posicionamiento y generación
            BlockVector3 pastePos = calculateOptimalPosition(world, position, clipboard);
            return pasteStructureWithVisualFeedback(source, world, clipboard, pastePos);

        } catch (Exception e) {
            handleError(source, "Error al generar estructura", e);
            return false;
        }
    }

    private static void validateSource(ServerCommandSource source) {
        if (source == null || source.getWorld() == null) {
            throw new IllegalArgumentException("Fuente o mundo inválidos");
        }
    }

    private static Path getSchematicPath(String type, String material, String style) throws IOException {
        String filename = String.format("%s_%s_%s.schem", type, material, style);
        Path path = Paths.get(SCHEMATICS_DIR, filename);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("Esquema no encontrado: " + filename);
        }

        if (Files.size(path) == 0) {
            throw new IOException("El esquema está vacío");
        }

        return path;
    }

    private static Clipboard loadAndValidateSchematic(Path schematicPath) throws IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(schematicPath.toFile());
        if (format == null) {
            throw new IOException("Formato de esquema no reconocido");
        }

        try (ClipboardReader reader = format.getReader(Files.newInputStream(schematicPath))) {
            Clipboard clipboard = reader.read();

            if (isEmpty(clipboard)) {
                throw new IOException("El esquema no contiene bloques sólidos");
            }

            return clipboard;
        }
    }

    private static boolean isEmpty(Clipboard clipboard) {
        Region region = clipboard.getRegion();
        int solidBlocks = 0;

        for (BlockVector3 pos : region) {
            if (!isAirOrFoliage(clipboard.getBlock(pos))) {
                solidBlocks++;
                // Considerar válido si tiene al menos 1 bloque sólido
                if (solidBlocks >= 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isAirOrFoliage(BlockState block) {
        return NON_SOLID_BLOCKS.contains(block.getBlockType());
    }

    private static BlockVector3 calculateOptimalPosition(World world, BlockPos position, Clipboard clipboard) {
        BlockVector3 dimensions = clipboard.getDimensions();
        int groundY = findGroundLevel(world, position.getX(), position.getZ());
        int structureHeight = dimensions.y();

        // Ajuste para estructuras altas
        groundY = Math.min(groundY, world.getMaxY() - structureHeight);
        groundY = Math.max(groundY, world.getMinY());

        return BlockVector3.at(position.getX(), groundY, position.getZ());
    }

    private static int findGroundLevel(World world, int x, int z) {
        // Búsqueda bidireccional optimizada
        for (int y = 63; y < world.getMaxY(); y++) {
            if (isSolidSurface(world, x, y, z)) {
                return y + 1;
            }
        }

        for (int y = 62; y >= world.getMinY(); y--) {
            if (isSolidSurface(world, x, y, z)) {
                return y + 1;
            }
        }

        return 64; // Nivel por defecto
    }

    private static boolean isSolidSurface(World world, int x, int y, int z) {
        BlockVector3 pos = BlockVector3.at(x, y, z);
        BlockState block = world.getBlock(pos);
        return !NON_SOLID_BLOCKS.contains(block.getBlockType());
    }

    private static boolean pasteStructureWithVisualFeedback(ServerCommandSource source, World world,
                                                            Clipboard clipboard, BlockVector3 pastePos)
            throws WorldEditException {
        try (EditSession editSession = createEditSession(world)) {
            // Marcador visual
            editSession.setBlock(pastePos, BlockTypes.GLOWSTONE.getDefaultState());

            // Operación de pegado
            Operation pasteOp = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(pastePos)
                    .ignoreAirBlocks(false)
                    .build();

            Operations.complete(pasteOp);

            // Feedback visual y auditivo
            sendSuccessFeedback(source, pastePos, clipboard);
            return true;
        }
    }

    private static EditSession createEditSession(World world) {
        return WorldEdit.getInstance().newEditSessionBuilder()
                .world(world)
                .maxBlocks(-1)
                .build();
    }

    private static void sendSuccessFeedback(ServerCommandSource source, BlockVector3 pos, Clipboard clipboard) {
        BlockVector3 dim = clipboard.getDimensions();
        source.sendMessage(Text.literal("✓ Estructura generada con éxito!"));
        source.sendMessage(Text.literal(String.format(
                "Ubicación: X=%d Y=%d Z=%d | Dimensiones: %dx%dx%d",
                pos.x(), pos.y(), pos.z(),
                dim.x(), dim.y(), dim.z()
        )));

        spawnParticles(source.getWorld(), pos, dim);
    }

    private static void spawnParticles(ServerWorld world, BlockVector3 center, BlockVector3 size) {
        BlockPos pos = new BlockPos(center.x(), center.y(), center.z());
        double centerX = pos.getX() + size.x() / 2.0;
        double centerY = pos.getY() + size.y() / 2.0;
        double centerZ = pos.getZ() + size.z() / 2.0;

        world.spawnParticles(ParticleTypes.END_ROD,
                centerX, centerY, centerZ,
                30,
                size.x() / 4.0, size.y() / 4.0, size.z() / 4.0,
                0.1);
    }

    private static void handleError(ServerCommandSource source, String message, Exception e) {
        LOGGER.error(message, e);
        source.sendMessage(Text.literal("✗ " + message + ": " + e.getMessage()));
    }

    // Métodos utilitarios adicionales
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
}