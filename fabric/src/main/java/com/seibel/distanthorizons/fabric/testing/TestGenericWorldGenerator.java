package com.seibel.distanthorizons.fabric.testing;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.EDhApiDetailLevel;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGeneratorReturnType;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import com.seibel.distanthorizons.api.objects.data.IDhApiFullDataSource;
import com.seibel.distanthorizons.common.wrappers.block.BiomeWrapper;
import com.seibel.distanthorizons.common.wrappers.block.BlockStateWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.GenerationEvent;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.dataObjects.transformers.LodDataBuilder;
import com.seibel.distanthorizons.core.level.IDhServerLevel;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.objects.Pair;
import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayList.PhantomArrayListCheckout;
import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayList.PhantomArrayListPool;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import com.seibel.distanthorizons.coreapi.util.BitShiftUtil;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import com.seibel.distanthorizons.core.logging.DhLogger;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class TestGenericWorldGenerator implements IDhApiWorldGenerator
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private final IServerLevelWrapper serverLevelWrapper;
	
	private static final PhantomArrayListPool ARRAY_LIST_POOL = new PhantomArrayListPool("TestWorldGen");
	private final BatchGenerationEnvironment batchGenerator;
	private static final ConcurrentHashMap<IBiomeWrapper, IBlockStateWrapper> BIOME_TO_BLOCK_WRAPPER = new ConcurrentHashMap<>();
	
	// commonly used blocks cached for quick access
	private final IBlockStateWrapper waterBlock;
	private final IBlockStateWrapper iceBlock;
	private final IBlockStateWrapper snowBlock;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public TestGenericWorldGenerator(IDhApiLevelWrapper serverLevelWrapper)
	{ 
		this.serverLevelWrapper = (IServerLevelWrapper) serverLevelWrapper;
		IDhServerLevel serverLevel = (IDhServerLevel)this.serverLevelWrapper.getDhLevel();
		
		this.batchGenerator = new BatchGenerationEnvironment(serverLevel);
		
		
		this.waterBlock = BlockStateWrapper.getWaterBlockStateWrapper(this.serverLevelWrapper);
		this.iceBlock = BlockStateWrapper.getIceBlockStateWrapper(this.serverLevelWrapper);
		this.snowBlock = BlockStateWrapper.getSnowBlockStateWrapper(this.serverLevelWrapper);
	}
	
	//endregion
	
	
	
	//============//
	// properties //
	//============//
	//region
	
	@Override
	public byte getSmallestDataDetailLevel() { return (byte) (EDhApiDetailLevel.BLOCK.detailLevel); }
	@Override
	public byte getLargestDataDetailLevel()
	{ return (byte) (EDhApiDetailLevel.BLOCK.detailLevel + 12); }
	
	
	@Override
	public EDhApiWorldGeneratorReturnType getReturnType() { return EDhApiWorldGeneratorReturnType.API_DATA_SOURCES; }
	
	@Override
	public boolean runApiValidation() { return true; }
	
	//endregion
	
	
	
	//==================//
	// chunk generation //
	//==================//
	//region
	
	@Override
	public void preGeneratorTaskStart() { /* do nothing */ }
	
	
	
	@Override
	public CompletableFuture<Void> generateLod(
		int chunkPosMinX, int chunkPosMinZ,
		int posX, int posZ, byte detailLevel,
		IDhApiFullDataSource pooledFullDataSource,
		EDhApiDistantGeneratorMode generatorMode, ExecutorService worldGeneratorThreadPool,
		Consumer<IDhApiFullDataSource> resultConsumer)
	{
		return CompletableFuture.runAsync(() ->
				this.generateInternal(
					chunkPosMinX, chunkPosMinZ,
					posX, posZ, detailLevel,
					pooledFullDataSource, 
					generatorMode, worldGeneratorThreadPool,
					resultConsumer),
			worldGeneratorThreadPool);
	}
	public void generateInternal(
		int chunkPosMinX, int chunkPosMinZ,
		int posX, int posZ, byte detailLevel,
		IDhApiFullDataSource pooledFullDataSource,
		EDhApiDistantGeneratorMode generatorMode, ExecutorService worldGeneratorThreadPool,
		Consumer<IDhApiFullDataSource> resultConsumer)
	{
		// this test is only validated for 1.18.2 and up 
		// (and it is only needed when testing world gen overrides/API chunks, so it isn't normally needed)
		#if MC_VER >= MC_1_18_2
		
		
		if (detailLevel == 0)
		{
			ArrayList<IChunkWrapper> chunkList = new ArrayList<>(16);
			
			DhChunkPos chunkPos = new DhChunkPos(chunkPosMinX, chunkPosMinZ);
			
			GenerationEvent genEvent = new GenerationEvent(
				chunkPos, 4,
				this.batchGenerator,
				EDhApiDistantGeneratorMode.FEATURES, EDhApiWorldGenerationStep.FEATURES,
				(chunkWrapper) ->
				{
					if (chunkWrapper != null)
						chunkList.add(chunkWrapper);
				}
			);
			this.batchGenerator.generateEvent(genEvent);
			
			for (int i = 0; i < chunkList.size(); i++)
			{
				IChunkWrapper chunkWrapper = chunkList.get(i);
				try (FullDataSourceV2 dataSource = LodDataBuilder.createFromChunk((IServerLevelWrapper)this.serverLevelWrapper, chunkWrapper))
				{
					((FullDataSourceV2)pooledFullDataSource).updateFromDataSource(dataSource);
				}
			}
			resultConsumer.accept(pooledFullDataSource);
			
			return;
		}
		
		
		
		//=====================//
		// noise gen variables //
		//=====================//
		//region
		
		ServerLevel level = ((ServerLevel)this.serverLevelWrapper.getWrappedMcObject());
		RandomState randomState = level.getChunkSource().randomState();
		DensityFunction finalDensity = randomState.router().finalDensity();
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		BiomeSource biomeSource = generator.getBiomeSource();
		
		int relativeSeaLevel = level.getSeaLevel() - level.getMinY();
		
		//endregion
		
		
		
		
		
		ArrayList<DhApiTerrainDataPoint> dataPoints = new ArrayList<>();
		int width = pooledFullDataSource.getWidthInDataColumns();
		
		try(PhantomArrayListCheckout checkout = ARRAY_LIST_POOL.checkoutLongArrays(1))
		{
			LongArrayList heightmap = checkout.getLongArray(0, width * width);
			for (int x = 0; x < width; x++)
			{
				for (int z = 0; z < width; z++)
				{
					heightmap.set(x + width * z, Long.MIN_VALUE);
				}
			}
			
			// only generate heights for 1 in 4 columns
			for (int x = 0; x < width; x+=2)
			{
				for (int z = 0; z < width; z+=2)
				{
					// convert to block pos
					int blockX = chunkPosMinX * 16 + (x * BitShiftUtil.powerOfTwo(detailLevel)); // TODO is there better logic than just doing power-of-two?
					int blockZ = chunkPosMinZ * 16 + (z * BitShiftUtil.powerOfTwo(detailLevel));
					
					int maxHeight = findSurfaceHeight(finalDensity, this.serverLevelWrapper, blockX, blockZ, relativeSeaLevel);
					maxHeight -= this.serverLevelWrapper.getMinHeight(); // convert to level relative position
					
					heightmap.set(x + width * z, maxHeight);
				}
			}
			
			
			
			for (int x = 0; x < width; x++)
			{
				for (int z = 0; z < width; z++)
				{
					dataPoints.clear();
					
					// convert to block pos
					int blockX = chunkPosMinX * 16 + (x * BitShiftUtil.powerOfTwo(detailLevel)); // TODO is there better logic than just doing power-of-two?
					int blockZ = chunkPosMinZ * 16 + (z * BitShiftUtil.powerOfTwo(detailLevel));
					
					
					// get height
					long maxHeightLong = heightmap.getLong(x + width * z);
					if (maxHeightLong == Long.MIN_VALUE)
					{
						int x0 = (x / 2) * 2;
						int z0 = (z / 2) * 2;
						// clamp to width - 2 since that's the last known (even) column/row
						int x1 = Math.min(x0 + 2, width - 2);
						int z1 = Math.min(z0 + 2, width - 2);
						
						long h00 = heightmap.get(x0 + width * z0);
						long h10 = heightmap.get(x1 + width * z0);
						long h01 = heightmap.get(x0 + width * z1);
						long h11 = heightmap.get(x1 + width * z1);
						
						double xLerp = (x1 != x0) ? (double) (x - x0) / (x1 - x0) : 0.0;
						double zLerp = (z1 != z0) ? (double) (z - z0) / (z1 - z0) : 0.0;
						
						double top = h00 + (h10 - h00) * xLerp;
						double bottom = h01 + (h11 - h01) * xLerp;
						double interpolated = top + (bottom - top) * zLerp;
						
						maxHeightLong = Math.round(interpolated);
					}
					
					int surfaceHeight = (int) maxHeightLong;
					int waterHeight = Integer.MIN_VALUE; // TODO
					if (surfaceHeight < relativeSeaLevel)
					{
						waterHeight = relativeSeaLevel;
					}
					
					// get biome
					Holder<Biome> biomeHolder = biomeSource.getNoiseBiome(
						QuartPos.fromBlock(blockX), // x
						QuartPos.fromBlock(surfaceHeight), // y
						QuartPos.fromBlock(blockZ), // z
						randomState.sampler()
					);
					IBiomeWrapper biomeWrapper = BiomeWrapper.getBiomeWrapper(biomeHolder, this.serverLevelWrapper);
					boolean isColdBiome = biomeHolder.value().getBaseTemperature() < 0.1f; // https://minecraft.wiki/w/Biome#Temperature
					
					
					// get block
					IBlockStateWrapper surfaceBlock
						= this.getSurfaceBlockState(
							biomeWrapper,
							blockX, blockZ);
					
					//// TODO remove border logic once getSurfaceBlockState() has been implemented
					//if (x == 0 || x == (width-1)
					//	|| z == 0 || z == (width-1))
					//{
					//	// using a border block makes it easier to see different sections being generated
					//	surfaceBlock = borderBlock;
					//}
					
					
					
					// sky lighting can be ignored. DH will auto light the LODs after they've been submitted
					// block lighting however will need to be generated here
					{
						int surfaceSkyLight = LodUtil.MAX_MC_LIGHT;
						if (waterHeight != Integer.MIN_VALUE)
						{
							surfaceSkyLight -= (waterHeight - surfaceHeight);
							if (surfaceSkyLight < LodUtil.MIN_MC_LIGHT)
							{
								surfaceSkyLight = LodUtil.MIN_MC_LIGHT;
							}
						}
						else
						{
							// uncovered surface blocks use snow in cold biomes
							if (isColdBiome)
							{
								surfaceBlock = snowBlock;
							}
						}
						
						
						// surface
						dataPoints.add(DhApiTerrainDataPoint.create((byte) 0, surfaceBlock.getLightEmission(), surfaceSkyLight, 0, surfaceHeight,
							surfaceBlock, biomeWrapper));
						
						
						// optional water
						if (waterHeight != Integer.MIN_VALUE)
						{
							if (isColdBiome)
							{
								int waterHeightDiff = (waterHeight - surfaceHeight);
								if (waterHeightDiff >= 2)
								{
									// under-ice water
									dataPoints.add(DhApiTerrainDataPoint.create((byte) 0, 0, LodUtil.MAX_MC_LIGHT - 1, surfaceHeight, waterHeight - 1,
										waterBlock, biomeWrapper));
									
									// surface ice
									dataPoints.add(DhApiTerrainDataPoint.create((byte) 0, 0, LodUtil.MAX_MC_LIGHT, waterHeight - 1, waterHeight,
										iceBlock, biomeWrapper));
								}
								else if (waterHeightDiff == 1)
								{
									// ice 
									dataPoints.add(DhApiTerrainDataPoint.create((byte) 0, 0, LodUtil.MAX_MC_LIGHT, surfaceHeight, waterHeight,
										iceBlock, biomeWrapper));
								}
							}
							else
							{
								dataPoints.add(DhApiTerrainDataPoint.create((byte) 0, 0, LodUtil.MAX_MC_LIGHT, surfaceHeight, waterHeight,
									waterBlock, biomeWrapper));
							}
							
							
							surfaceHeight = waterHeight;
						}
						
						// air
						dataPoints.add(DhApiTerrainDataPoint.create((byte) 0, 0, LodUtil.MAX_MC_LIGHT, surfaceHeight, this.serverLevelWrapper.getMaxHeight(), // TODO does level max height need to be offset by level min height? probably
							BlockStateWrapper.AIR, biomeWrapper));
						
						try
						{
							pooledFullDataSource.setApiDataPointColumn(x, z, EDhApiWorldGenerationStep.SURFACE, dataPoints);
						}
						catch (Exception e)
						{
							throw e;
						}
					}
				}
			}
		}
		
		resultConsumer.accept(pooledFullDataSource);
			
		#else
		#endif
	}
	
	//endregion
	
	
	
	//=====================//
	// block getting logic //
	//=====================//
	//region
	
	private IBlockStateWrapper getSurfaceBlockState(
		IBiomeWrapper biomeWrapper,
		int blockX, int blockZ)
	{
		HashMap<IBiomeWrapper, HashMap<IBlockStateWrapper, Integer>> biomeBlockCounts = new HashMap<>();
		
		IBlockStateWrapper blockState = BIOME_TO_BLOCK_WRAPPER.get(biomeWrapper);
		if (blockState == null)
		{
			Consumer<IChunkWrapper> resultConsumer = (chunkWrapper) -> 
			{
				for(int x = 0; x < LodUtil.CHUNK_WIDTH; x++)
				{
					for(int z = 0; z < LodUtil.CHUNK_WIDTH; z++)
					{
						int height = chunkWrapper.getSolidHeightMapValue(x, z);
						IBiomeWrapper biome = chunkWrapper.getBiome(x, height, z);
						IBlockStateWrapper block = chunkWrapper.getBlockState(x, height, z);
						
						HashMap<IBlockStateWrapper, Integer> blockCounts = biomeBlockCounts.computeIfAbsent(biome, b -> new HashMap<>());
						blockCounts.merge(block, 1, Integer::sum);
					}
				}
			};
			DhChunkPos chunkPos = new DhChunkPos(new DhBlockPos2D(blockX, blockZ));
			chunkPos = new DhChunkPos(chunkPos.getX() -1 , chunkPos.getZ() -1);
			GenerationEvent genEvent = new GenerationEvent(
				chunkPos, 4,
				this.batchGenerator,
				EDhApiDistantGeneratorMode.SURFACE, EDhApiWorldGenerationStep.SURFACE,
				resultConsumer);
			this.batchGenerator.generateEvent(genEvent);
			
			
			
			for (IBiomeWrapper biome : biomeBlockCounts.keySet())
			{
				Pair<IBlockStateWrapper, Integer> pair = this.getMostCommonBlockForBiome(biomeBlockCounts, biome);
				IBlockStateWrapper block = pair.first;
				int count = pair.second;
				if (count > 8
					&& !BIOME_TO_BLOCK_WRAPPER.containsKey(biome))
				{
					BIOME_TO_BLOCK_WRAPPER.put(biome, block);
					
					if (biomeWrapper.equals(biome))
					{
						blockState = block;
					}
				}
			}
			
			if (blockState == null)
			{
				blockState = BIOME_TO_BLOCK_WRAPPER.get(biomeWrapper);
			}
			
		}
		
		if (blockState != null)
		{
			return blockState;
		}
		
		try
		{
			return BlockStateWrapper.deserialize("minecraft:pink_wool", this.serverLevelWrapper);
		}
		catch (IOException e)
		{
			LOGGER.error("failed to get block: " + e.getMessage(), e);
			return BlockStateWrapper.AIR;
		}
	}
	
	private Pair<IBlockStateWrapper, Integer> getMostCommonBlockForBiome(
		HashMap<IBiomeWrapper, HashMap<IBlockStateWrapper, Integer>> biomeBlockCounts, IBiomeWrapper biome)
	{
		HashMap<IBlockStateWrapper, Integer> blockCounts = biomeBlockCounts.get(biome);
		if (blockCounts == null || blockCounts.isEmpty())
		{
			return null;
		}
		
		IBlockStateWrapper mostCommonBlock = null;
		int highestCount = -1;
		
		for (HashMap.Entry<IBlockStateWrapper, Integer> entry : blockCounts.entrySet())
		{
			if (entry.getValue() > highestCount)
			{
				highestCount = entry.getValue();
				mostCommonBlock = entry.getKey();
			}
		}
		
		return new Pair<>(mostCommonBlock, highestCount);
	}
	
	//endregion
	
	
	
	//=====================//
	// noise surface logic //
	//=====================//
	//region
	
	// originally based on world gen logic
	// from the LOD mod "Ecstatic"
	// https://www.curseforge.com/minecraft/mc-mods/ecstatic
	
	// tested on MC 26.1.2 and 26.2.0
	
	private static final int MARCH_STEP = 8;
	public static final int NO_HEIGHT_HINT = Integer.MIN_VALUE; // no neighboring point to check against for floating points
	private static final int FLOATING_OUTLIER_THRESHOLD_BLOCKS = 24; // maximum height diff between neighboring points before being treated as a floating block/blob
	
	private static int findSurfaceHeight(
		DensityFunction finalDensity, 
		ILevelWrapper levelWrapper, 
		int blockX, int blockZ,
		int seaLevel)
	{
		int nonSolidY = levelWrapper.getMaxHeight(); // known non-solid (top of world)
		int solidY = levelWrapper.getMinHeight() - 1; // assume bottom-most is solid; adjust if not guaranteed
		
		// Edge case: if even the top is solid, or bottom isn't solid, handle explicitly
		if (isSolid(finalDensity, blockX, nonSolidY, blockZ))
		{
			return nonSolidY + 1; // or whatever convention you want for "world is solid at max height"
		}
		
		while (nonSolidY - solidY > 1)
		{
			int mid = (int)((nonSolidY / 2.0) + (solidY / 2.0)); // doing int division can cause the same number to be returned, causing an infinite loop. TODO can that happen here anyway?
			if (isSolid(finalDensity, blockX, mid, blockZ))
			{
				solidY = mid;
			}
			else
			{
				nonSolidY = mid;
			}
		}
		
		return solidY + 1; // first air block above the ground, consistent with your existing convention
	}
	
	private static int findSurfaceHeight_walk(
		DensityFunction finalDensity, 
		ILevelWrapper levelWrapper, 
		int blockX, int blockZ, int expectedHeightHint)
	{
		int top = levelWrapper.getMaxHeight(); // TODO getting cut off at ~120 Y
		int bottom = levelWrapper.getMinHeight();
		int prevY = top;
		int y = top - MARCH_STEP;
		int fallback = bottom;
		
		while (y >= bottom)
		{
			if (isSolid(finalDensity, blockX, y, blockZ))
			{
				int candidate = binaryRefine(finalDensity, blockX, blockZ, prevY, y) + 1;
				if (expectedHeightHint == NO_HEIGHT_HINT
					|| Math.abs(candidate - expectedHeightHint) <= FLOATING_OUTLIER_THRESHOLD_BLOCKS)
				{
					return candidate;
				}

				fallback = candidate;
				prevY = candidate - 1;
				y = prevY - MARCH_STEP;
			}
			else
			{
				prevY = y;
				y -= MARCH_STEP;
			}
		}
		
		return fallback;
	}
	private static int binaryRefine(DensityFunction finalDensity, int blockX, int blockZ, int highNonSolidY, int lowSolidY)
	{
		while (highNonSolidY - lowSolidY > 1)
		{
			int mid = (highNonSolidY + lowSolidY) >>> 1;
			if (isSolid(finalDensity, blockX, mid, blockZ))
			{
				lowSolidY = mid;
			}
			else
			{
				highNonSolidY = mid;
			}
		}
		
		return lowSolidY;
	}
	private static boolean isSolid(DensityFunction finalDensity, int blockX, int blockY, int blockZ)
	{ return finalDensity.compute(new DensityFunction.SinglePointContext(blockX, blockY, blockZ)) > 0.0; }
	
	//endregion
	
	
	
	//=========//
	// cleanup //
	//=========//
	//region
	
	@Override
	public void close() { /* do nothing */ }
	
	//endregion
	
	
	
}
