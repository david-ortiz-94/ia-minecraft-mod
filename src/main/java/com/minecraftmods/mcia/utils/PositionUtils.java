package com.minecraftmods.mcia.utils;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import net.minecraft.util.math.BlockPos;

public class PositionUtils {
    public static BlockVector3 calculatePastePosition(World world, BlockPos position, Clipboard clipboard) {
        BlockVector3 dimensions = clipboard.getDimensions();
        int groundY = findGroundLevel(world, position.getX(), position.getZ());
        int structureHeight = dimensions.y();

        groundY = Math.min(groundY, world.getMaxY() - structureHeight);
        groundY = Math.max(groundY, world.getMinY());

        return BlockVector3.at(position.getX(), groundY, position.getZ());
    }

    public static int findGroundLevel(World world, int x, int z) {
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

        return 64;
    }

    private static boolean isSolidSurface(World world, int x, int y, int z) {
        return BlockUtils.isSolid(world.getBlock(BlockVector3.at(x, y, z)));
    }

}