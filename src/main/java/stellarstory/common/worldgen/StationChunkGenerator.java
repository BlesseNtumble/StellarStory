package stellarstory.common.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import stellarstory.StellarStory;
import stellarstory.api.celestial.basic_type.Station;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class StationChunkGenerator extends ChunkGenerator {

    public static final Codec<StationChunkGenerator> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                          BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
                    ).apply(instance, StationChunkGenerator::new)
            );

    public StationChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion p_223043_, long p_223044_, RandomState p_223045_, BiomeManager p_223046_, StructureManager p_223047_, ChunkAccess p_223048_, GenerationStep.Carving p_223049_) {}

    @Override
    public void buildSurface(WorldGenRegion p_223050_, StructureManager p_223051_, RandomState p_223052_, ChunkAccess p_223053_) {}

    @Override
    public void spawnOriginalMobs(WorldGenRegion p_62167_) {}


    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                        RandomState randomState, StructureManager structureManager,
                                                        ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }
//
//    @Override
//    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess) {
//
//        if (chunkAccess instanceof ProtoChunk protoChunk) {
//            // В Forge 1.20.1 используем getWorldForge() для получения LevelAccessor
//            LevelAccessor levelAccessor = protoChunk.getWorldForge();
//
//            // Менеджер структур есть только в Level, поэтому приводим тип
//            if (levelAccessor instanceof Level level && levelAccessor instanceof ServerLevelAccessor serverLevelAccessor) {
//                ChunkPos chunkPos = protoChunk.getPos();
//
//                // Генерируем структуру только в центральном чанке (0, 0)
//                if (level.getServer() != null && chunkPos.x == 0 && chunkPos.z == 0) {
//                    StructureTemplateManager templateManager = level.getServer().getStructureManager();
//                    Optional<StructureTemplate> template = templateManager.get(structure.get());
//                    template.ifPresent(temp -> {
//                        // Y=64 — центр станции
//                        BlockPos startPos = new BlockPos(0, 64, 0);
//                        RandomSource random = RandomSource.create(
//                                structure.get().hashCode()
//                        );
//
//                        var settings = new StructurePlaceSettings();
//                        // Размещаем NBT-структуру
//                        temp.placeInWorld(serverLevelAccessor, startPos, startPos, settings, random, 3);
//                    });
//                }
//            }
//        }
//        return CompletableFuture.completedFuture(chunkAccess);
//    }


    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor world, RandomState random) {
        return (x == 0 && z == 0) ? 128 : 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world, RandomState random) {
        int yStart = world.getMinBuildHeight();
        int yEnd = world.getMaxBuildHeight();
        BlockState[] column = new BlockState[yEnd - yStart];
        for (int i = 0; i < column.length; i++) {
            column[i] = Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(yStart, column);
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos pos) {

    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getGenDepth() {
        // должно совпадать с height в dimension_type/station_type.json
        return 256;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }


}
