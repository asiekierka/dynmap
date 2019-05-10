package org.dynmap.fabric_1_14_1;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionType;
import org.dynmap.DynmapChunk;
import org.dynmap.DynmapLocation;
import org.dynmap.DynmapWorld;
import org.dynmap.utils.MapChunkCache;
import org.dynmap.utils.Polygon;

import java.util.List;

public class FabricWorld extends DynmapWorld {
    final World world;

    protected FabricWorld(World world) {
        super(getName(world.dimension.getType()), world.getHeight(), world.getSeaLevel());
        this.world = world;
    }

    private static String getName(DimensionType type) {
        return "world" + type.getSuffix();
    }

    @Override
    public boolean isNether() {
        return world.dimension.isNether();
    }

    @Override
    public DynmapLocation getSpawnLocation() {
        BlockPos pos = world.getSpawnPos();
        return new DynmapLocation(getName(), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public long getTime() {
        return world.getTime();
    }

    @Override
    public boolean hasStorm() {
        return false; // TODO
    }

    @Override
    public boolean isThundering() {
        return world.isThundering();
    }

    @Override
    public boolean isLoaded() {
        return true; // TODO
    }

    @Override
    public void setWorldUnloaded() {
        // TODO
    }

    @Override
    public int getLightLevel(int x, int y, int z) {
        return world.getLightLevel(new BlockPos(x, y, z));
    }

    @Override
    public int getHighestBlockYAt(int x, int z) {
        BlockPos pos = new BlockPos(x, worldheight - 1, z);
        return world.getChunk(pos).getHeightmap(Heightmap.Type.MOTION_BLOCKING).get(x & 15, z & 15);
    }

    @Override
    public boolean canGetSkyLightLevel() {
        return true;
    }

    @Override
    public int getSkyLightLevel(int x, int y, int z) {
        return world.getLightLevel(LightType.SKY, new BlockPos(x, y, z));
    }

    @Override
    public String getEnvironment() {
        return "TODO"; // TODO
    }

    @Override
    public MapChunkCache getChunkCache(List<DynmapChunk> chunks) {
        return new FabricMapChunkCache(this, this.world, chunks); // TODO
    }

    @Override
    public Polygon getWorldBorder() {
        if (world != null) {
            WorldBorder wb = world.getWorldBorder();
            if ((wb != null) && (wb.getSize() < 5.9E7)) {
                Polygon p = new Polygon();
                p.addVertex(wb.getBoundWest(), wb.getBoundNorth());
                p.addVertex(wb.getBoundWest(), wb.getBoundSouth());
                p.addVertex(wb.getBoundEast(), wb.getBoundSouth());
                p.addVertex(wb.getBoundEast(), wb.getBoundNorth());
                return p;
            }
        }
        return null;
    }
}
