package org.dynmap.fabric_1_14_1;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.dynmap.DynmapChunk;
import org.dynmap.DynmapWorld;
import org.dynmap.common.BiomeMap;
import org.dynmap.renderer.DynmapBlockState;
import org.dynmap.renderer.RenderPatchFactory;
import org.dynmap.utils.BlockStep;
import org.dynmap.utils.MapChunkCache;
import org.dynmap.utils.MapIterator;
import org.dynmap.utils.VisibilityLimit;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FabricMapChunkCache extends MapChunkCache {
    private final FabricWorld fworld;
    private final ServerWorld world;
    private final ServerChunkManager chunkManager;
    private final LinkedList<ChunkPos> chunksToLoad;
    private final List<CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> chunkFutures;
    private final Map<ChunkPos, Chunk> chunks;
    private ChunkStatus requiredStatus;

    public FabricMapChunkCache(FabricWorld fworld, World world, List<DynmapChunk> chunks) {
        this.fworld = fworld;
        this.world = (ServerWorld) world;
        this.chunkManager = (ServerChunkManager) this.world.getChunkManager();
        this.chunksToLoad = new LinkedList<>();
        this.chunkFutures = new ArrayList<>();
        this.chunks = new HashMap<>();
        this.requiredStatus = ChunkStatus.FULL;

        for (DynmapChunk c : chunks) {
            chunksToLoad.add(new ChunkPos(c.x, c.z));
        }
    }

    @Override
    public boolean setChunkDataTypes(boolean blockdata, boolean biome, boolean highestblocky, boolean rawbiome) {
        requiredStatus = ChunkStatus.FEATURES;

        if (highestblocky) {
            requiredStatus = ChunkStatus.FULL;
        }

        return true;
    }

    @Override
    public int loadChunks(int maxToLoad) {
        maxToLoad = Math.min(chunksToLoad.size(), maxToLoad);
        for (int i = 0; i < maxToLoad; i++) {
            ChunkPos pos = chunksToLoad.remove();
            chunkFutures.add(chunkManager.getChunkFutureSyncOnMainThread(pos.x, pos.z, requiredStatus, false));
        }

        return chunks.size() + maxToLoad;
    }

    @Override
    public boolean isDoneLoading() {
        Iterator<CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> futureIterator = chunkFutures.iterator();
        while (futureIterator.hasNext()) {
            CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> future = futureIterator.next();
            if (future.isDone()) {
                futureIterator.remove();
                try {
                    future.get().ifLeft(c -> chunks.put(c.getPos(), c));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return chunkFutures.isEmpty();
    }

    @Override
    public boolean isEmpty() {
        return chunks.isEmpty();
    }

    @Override
    public void unloadChunks() {
        chunkFutures.clear();
        chunks.clear();
    }

    @Override
    public boolean isEmptySection(int sx, int sy, int sz) {
        Chunk c = chunks.get(new ChunkPos(sx, sz));
        if (c != null) {
            return c.getSectionArray()[sy].isEmpty();
        }
        return false;
    }

    private static final BlockStep[] unstep = {BlockStep.X_MINUS, BlockStep.Y_MINUS, BlockStep.Z_MINUS,
            BlockStep.X_PLUS, BlockStep.Y_PLUS, BlockStep.Z_PLUS
    };

    @Override
    public MapIterator getIterator(int x, int y, int z) {
        return new MapIterator() {
            private BlockStep lastStep;
            private BlockPos.Mutable pos;

            @Override
            public void initialize(int x0, int y0, int z0) {
                this.pos = new BlockPos.Mutable(x0, y0, z0);
            }

            @Override
            public int getBlockSkyLight() {
                return world.getLightLevel(LightType.SKY, pos);
            }

            @Override
            public int getBlockEmittedLight() {
                return world.getLightLevel(LightType.BLOCK, pos);
            }

            @Override
            public BiomeMap getBiome() {
                return BiomeMap.byBiomeID(Registry.BIOME.getRawId(world.getBiome(pos)));
            }

            @Override
            public int getSmoothGrassColorMultiplier(int[] colormap) {
                return world.getBiome(pos).getGrassColorAt(pos); // TODO
            }

            @Override
            public int getSmoothFoliageColorMultiplier(int[] colormap) {
                return world.getBiome(pos).getFoliageColorAt(pos); // TODO
            }

            @Override
            public int getSmoothWaterColorMultiplier() {
                return world.getBiome(pos).getWaterColor(); // TODO
            }

            @Override
            public int getSmoothWaterColorMultiplier(int[] colormap) {
                return world.getBiome(pos).getWaterColor(); // TODO
            }

            @Override
            public int getSmoothColorMultiplier(int[] colormap, int[] swampcolormap) {
                return world.getBiome(pos).getGrassColorAt(pos); // TODO
            }

            @Override
            public void stepPosition(BlockStep step) {
                lastStep = step;
                pos.setOffset(step.xoff, step.yoff, step.zoff);
            }

            @Override
            public void unstepPosition(BlockStep step) {
                stepPosition(unstep[step.ordinal()]);
            }

            @Override
            public BlockStep unstepPosition() {
                BlockStep ls = lastStep;
                stepPosition(unstep[lastStep.ordinal()]);
                return ls;
            }

            @Override
            public void setY(int y) {
                if (y == this.pos.getY()) return;

                lastStep = (y > this.pos.getY()) ? BlockStep.Y_PLUS : BlockStep.Y_MINUS;
                this.pos.setY(y);
            }

            @Override
            public DynmapBlockState getBlockTypeAt(BlockStep s) {
                BlockPos poso = pos.add(s.xoff, s.yoff, s.zoff);
                return FabricBlockStateMapper.INSTANCE.get(world.getBlockState(poso));
            }

            @Override
            public BlockStep getLastStep() {
                return lastStep;
            }

            @Override
            public int getWorldHeight() {
                return fworld.worldheight;
            }

            @Override
            public long getBlockKey() {
                return pos.asLong();
            }

            @Override
            public boolean isEmptySection() {
                return world.getChunk(pos).getSectionArray()[pos.getY() >> 4].isEmpty();
            }

            @Override
            public long getInhabitedTicks() {
                return world.getChunk(pos).getInhabitedTime();
            }

            @Override
            public RenderPatchFactory getPatchFactory() {
                return null;
            }

            @Override
            public DynmapBlockState getBlockType() {
                return FabricBlockStateMapper.INSTANCE.get(world.getBlockState(pos));
            }

            @Override
            public Object getBlockTileEntityField(String fieldId) {
                // TODO
                return null;
            }

            @Override
            public DynmapBlockState getBlockTypeAt(int xoff, int yoff, int zoff) {
                BlockPos poso = pos.add(xoff, yoff, zoff);
                return FabricBlockStateMapper.INSTANCE.get(world.getBlockState(poso));
            }

            @Override
            public Object getBlockTileEntityFieldAt(String fieldId, int xoff, int yoff, int zoff) {
                BlockPos poso = pos.add(xoff, yoff, zoff);
                // TODO
                return null;
            }

            @Override
            public int getX() {
                return this.pos.getX();
            }

            @Override
            public int getY() {
                return this.pos.getY();
            }

            @Override
            public int getZ() {
                return this.pos.getZ();
            }
        };
    }

    @Override
    public void setHiddenFillStyle(HiddenChunkStyle style) {
        // TODO
    }

    @Override
    public void setVisibleRange(VisibilityLimit limit) {
        // TODO
    }

    @Override
    public void setHiddenRange(VisibilityLimit limit) {
        // TODO
    }

    @Override
    public DynmapWorld getWorld() {
        return fworld;
    }
}
