package com.minecraftmods.mcia.util;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class WorldUtil {
    public static boolean isPositionValid(ServerWorld world, BlockPos pos) {
        // Validar que la posición es adecuada para construcción
        return true;
    }

    public static BlockPos findSuitableLocation(ServerWorld world, BlockPos startPos, int radius) {
        // Buscar una ubicación adecuada cerca de startPos
        return startPos;
    }
}
