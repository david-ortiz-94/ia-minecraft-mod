package com.minecraftmods.mcia.utils;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class FeedbackUtils {
    public static void sendSuccessFeedback(ServerCommandSource source, BlockVector3 pos, Clipboard clipboard) {
        BlockVector3 dim = clipboard.getDimensions();

        // Mensajes de texto
        source.sendMessage(Text.literal("✓ Estructura generada con éxito!"));
        source.sendMessage(Text.literal(String.format(
                "Ubicación: X=%d Y=%d Z=%d | Dimensiones: %dx%dx%d",
                pos.x(), pos.y(), pos.z(),
                dim.x(), dim.y(), dim.z()
        )));

        // Efectos visuales
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
}