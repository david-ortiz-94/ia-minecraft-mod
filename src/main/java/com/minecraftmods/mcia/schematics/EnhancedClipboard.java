package com.minecraftmods.mcia.schematics;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;

/**
 * Implementación mejorada de Clipboard para manejo de esquemas con características adicionales.
 */
public class EnhancedClipboard extends AbstractDelegateExtent implements Clipboard {
    private final Region region;
    private BlockVector3 origin;
    private final boolean hasBiomes;

    public EnhancedClipboard(Extent extent, Region region, BlockVector3 origin, boolean hasBiomes) {
        super(extent);
        this.region = region;
        this.origin = origin;
        this.hasBiomes = hasBiomes;
    }

    @Override
    public Region getRegion() {
        return region.clone();
    }

    @Override
    public BlockVector3 getDimensions() {
        return region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
    }

    @Override
    public BlockVector3 getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(BlockVector3 origin) {
        this.origin = origin;
    }

    @Override
    public boolean hasBiomes() {
        return hasBiomes;
    }

    @Override
    public Clipboard transform(Transform transform) throws WorldEditException {
        return new TransformedClipboard(this, transform);
    }

    // Métodos adicionales para mejor funcionalidad

    /**
     * Verifica si el clipboard está vacío (solo contiene aire)
     */
    public boolean isEmpty() {
        for (BlockVector3 pos : region) {
            if (!getBlock(pos).getBlockType().getMaterial().isAir()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Obtiene el bloque predominante en el clipboard
     */
    public BlockState getPredominantBlock() {
        // Implementación para encontrar el bloque más común
        // ...
        return getBlock(region.getMinimumPoint()); // Ejemplo simplificado
    }

    /**
     * Calcula el volumen no aéreo del clipboard
     */
    public int getSolidVolume() {
        int count = 0;
        for (BlockVector3 pos : region) {
            if (!getBlock(pos).getBlockType().getMaterial().isAir()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Clona este clipboard
     */
    @Override
    public EnhancedClipboard clone() {
        return new EnhancedClipboard(getExtent(), region.clone(), origin, hasBiomes);
    }
}

/**
 * Implementación para clipboards transformados
 */
class TransformedClipboard extends EnhancedClipboard {
    private final Transform transform;

    public TransformedClipboard(Clipboard parent, Transform transform) {
        super(parent, parent.getRegion(), parent.getOrigin(), parent.hasBiomes());
        this.transform = transform;
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        BlockVector3 transformedPos = transform.apply(position.toVector3()).toBlockPoint();
        return super.getBlock(transformedPos);
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        if (!hasBiomes()) return super.getBiome(position);
        BlockVector3 transformedPos = transform.apply(position.toVector3()).toBlockPoint();
        return super.getBiome(transformedPos);
    }
}