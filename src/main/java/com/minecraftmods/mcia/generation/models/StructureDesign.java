package com.minecraftmods.mcia.generation.models;

import com.google.gson.JsonObject;
import com.sk89q.worldedit.math.BlockVector3;

public class StructureDesign {
    private final JsonObject rawDesign;
    private final BlockVector3 dimensions;

    public StructureDesign(JsonObject design) {
        this.rawDesign = design;
        JsonObject dims = design.getAsJsonObject("dimensions");
        this.dimensions = BlockVector3.at(
                dims.get("width").getAsInt(),
                dims.get("height").getAsInt(),
                dims.get("length").getAsInt()
        );
    }

    public JsonObject getRawDesign() {
        return rawDesign;
    }

    public BlockVector3 getDimensions() {
        return dimensions;
    }
}