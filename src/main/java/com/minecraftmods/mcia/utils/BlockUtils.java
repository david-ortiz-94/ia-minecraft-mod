package com.minecraftmods.mcia.utils;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Set;
import java.util.stream.Collectors;


public class BlockUtils {
    // Use the FULL minecraft:block_id names for consistency
    private static final Set<String> SOLID_BLOCKS = Set.of(
            "minecraft:stone_bricks",
            "minecraft:stone",
            "minecraft:cobblestone",
            "minecraft:oak_log",
            "minecraft:spruce_planks",
            "minecraft:dark_oak_planks",
            "minecraft:bricks",
            "minecraft:quartz_block",
            "minecraft:andesite",
            "minecraft:spruce_log",
            "minecraft:oak_planks"
    );

    public static boolean isSolid(BlockState block) {
        if (block == null) return false;

        try {
            String fullId = block.getBlockType().id(); // Gets full ID like "minecraft:stone"
            System.out.println("Checking block: " + fullId); // Debug

            // Check both full ID and base ID (without properties)
            boolean isValid = SOLID_BLOCKS.contains(fullId.split("\\[")[0]);

            System.out.println("Is valid? " + isValid); // Debug
            return isValid;
        } catch (Exception e) {
            System.err.println("Error validating block: " + e.getMessage());
            return false;
        }
    }
}