package com.seibel.distanthorizons.common.wrappers.worldGeneration;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import com.seibel.distanthorizons.api.objects.data.IDhApiFullDataSource;
import com.seibel.distanthorizons.common.wrappers.block.BiomeWrapper;
import com.seibel.distanthorizons.common.wrappers.block.BlockStateWrapper;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayList.PhantomArrayListCheckout;
import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayList.PhantomArrayListPool;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.worldGeneration.IRoughGenerator;
import com.seibel.distanthorizons.coreapi.util.BitShiftUtil;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.WillNotClose;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DhRoughSurfaceGenerator implements IRoughGenerator
{
	public static final DhLogger LOGGER = new DhLoggerBuilder()
			.name("LOD World Gen - Rough Surface")
			.fileLevelConfig(Config.Common.Logging.logWorldGenEventToFile)
			.build();
	
	
	
	private final IServerLevelWrapper serverLevelWrapper;
	
	private static final PhantomArrayListPool ARRAY_LIST_POOL = new PhantomArrayListPool("TestWorldGen");
	private static final ConcurrentHashMap<IBiomeWrapper, BlockCountPair> BIOME_TO_BLOCK_WRAPPER = new ConcurrentHashMap<>();
	
	/**
	 * how far below the candidate height to check.
	 * By default MC samples noise on a 4x8x4 grid (source: BuilderB0y),
	 * so sampling 8 and 8x2 points down respectively should give us
	 * a pretty good guess if the datapoint is a small floating island or not.
	 */
	private static final int[] SANITY_CHECK_DEPTHS = { 4, 8, 16 };
	
	/** when marching down the world, this is how many blocks we should step at a time */
	private static final int MARCH_STEP = 8;
	
	
	// commonly used blocks cached for quick access
	private final IBlockStateWrapper waterBlock;
	private final IBlockStateWrapper iceBlock;
	private final IBlockStateWrapper snowBlock;
	
	/** needed to generate chunks surfaces to determine biome block mappings */
	@WillNotClose
	private final DhChunkGenerator batchGenerator;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public DhRoughSurfaceGenerator(IServerLevelWrapper serverLevelWrapper, DhChunkGenerator batchGenerator)
	{
		this.serverLevelWrapper = serverLevelWrapper;
		this.batchGenerator = batchGenerator;
		
		this.waterBlock = BlockStateWrapper.getWaterBlockStateWrapper(this.serverLevelWrapper);
		this.iceBlock = BlockStateWrapper.getIceBlockStateWrapper(this.serverLevelWrapper);
		this.snowBlock = BlockStateWrapper.getSnowBlockStateWrapper(this.serverLevelWrapper);
	}
	
	//endregion
	
	
	
	//============//
	// generation //
	//============//
	//region
	
	@Override
	public void generateSurface(
		int chunkPosMinX, int chunkPosMinZ,
		int posX, int posZ, byte detailLevel,
		IDhApiFullDataSource pooledFullDataSource,
		EDhApiDistantGeneratorMode generatorMode,
		Consumer<IDhApiFullDataSource> resultConsumer)
	{
		// this test is only validated for 1.18.2 and up 
		// (and it is only needed when testing world gen overrides/API chunks, so it isn't normally needed)
		#if MC_VER >= MC_1_18_2
		
		
		
		//=====================//
		// noise gen variables //
		//=====================//
		//region
		
		ServerLevel level = ((ServerLevel)this.serverLevelWrapper.getWrappedMcObject());
		RandomState randomState = level.getChunkSource().randomState();
		DensityFunction finalDensity = randomState.router().finalDensity();
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		BiomeSource biomeSource = generator.getBiomeSource();
		
		int relativeSeaLevel = this.serverLevelWrapper.getSeaLevel() - this.serverLevelWrapper.getMinHeight();
		int relativeMaxHeight = this.serverLevelWrapper.getMaxHeight() - this.serverLevelWrapper.getMinHeight();
		
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
					int blockX = chunkPosMinX * 16 + (x * BitShiftUtil.powerOfTwo(detailLevel));
					int blockZ = chunkPosMinZ * 16 + (z * BitShiftUtil.powerOfTwo(detailLevel));
					
					int maxHeight = findSurfaceHeight(finalDensity, this.serverLevelWrapper, blockX, blockZ);
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
					int blockX = chunkPosMinX * 16 + (x * BitShiftUtil.powerOfTwo(detailLevel));
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
								surfaceBlock = this.snowBlock;
							}
						}
						
						
						// surface
						if (surfaceHeight != 0) // will be 0 the column is only air (ie The End)
						{
							dataPoints.add(DhApiTerrainDataPoint.create((byte) 0, surfaceBlock.getLightEmission(), surfaceSkyLight, 0, surfaceHeight,
								surfaceBlock, biomeWrapper));
						}
						
						
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
										this.waterBlock, biomeWrapper));
									
									// surface ice
									dataPoints.add(DhApiTerrainDataPoint.create((byte) 0, 0, LodUtil.MAX_MC_LIGHT, waterHeight - 1, waterHeight,
										this.iceBlock, biomeWrapper));
								}
								else if (waterHeightDiff == 1)
								{
									// ice 
									dataPoints.add(DhApiTerrainDataPoint.create((byte) 0, 0, LodUtil.MAX_MC_LIGHT, surfaceHeight, waterHeight,
										this.iceBlock, biomeWrapper));
								}
							}
							else
							{
								dataPoints.add(DhApiTerrainDataPoint.create((byte) 0, 0, LodUtil.MAX_MC_LIGHT, surfaceHeight, waterHeight,
									this.waterBlock, biomeWrapper));
							}
							
							
							surfaceHeight = waterHeight;
						}
						
						// air
						dataPoints.add(DhApiTerrainDataPoint.create((byte) 0, 0, LodUtil.MAX_MC_LIGHT, surfaceHeight, relativeMaxHeight, // minus min height to convert 
							BlockStateWrapper.AIR, biomeWrapper));
						
						pooledFullDataSource.setApiDataPointColumn(x, z, EDhApiWorldGenerationStep.SURFACE, dataPoints);
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
		BlockCountPair existingBlockCountPair = BIOME_TO_BLOCK_WRAPPER.get(biomeWrapper);
		if (existingBlockCountPair != null)
		{
			return existingBlockCountPair.blockStateWrapper;
		}
		
		
		
		//
		// generate chunks to 
		// determine surface blocks //
		//
		
		HashMap<IBiomeWrapper, HashMap<IBlockStateWrapper, Integer>> biomeBlockCounts = new HashMap<>();
		{
			Consumer<IChunkWrapper> chunkResultConsumer = (chunkWrapper) ->
			{
				for (int x = 0; x < LodUtil.CHUNK_WIDTH; x++)
				{
					for (int z = 0; z < LodUtil.CHUNK_WIDTH; z++)
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
			// subtract 2 from each chunk pos so the target chunk is near the center
			chunkPos = new DhChunkPos(
				chunkPos.getX() - 2,
				chunkPos.getZ() - 2);
			ChunkGenEvent genEvent = new ChunkGenEvent(
				chunkPos,
				// 6 chunks wide mean we get 2 to 3 chunks of buffer, to get
				// a fairly large dataset of what the biome would be like
				6,
				this.batchGenerator,
				EDhApiDistantGeneratorMode.SURFACE, EDhApiWorldGenerationStep.SURFACE,
				chunkResultConsumer);
			this.batchGenerator.generateChunks(genEvent);
		}
		
		
		
		//
		// process generated biome/block pairs //
		//
		
		for (IBiomeWrapper biome : biomeBlockCounts.keySet())
		{
			BlockCountPair newPair = this.getMostCommonBlockForBiome(biomeBlockCounts, biome);
			if (newPair == null)
			{
				continue;
			}
			
			// require a moderate number of surface blocks be found
			// to prevent tiny biomes skewing the data
			if (newPair.count < 32)
			{
				continue;
			}
			
			
			// add this biome/block
			if (!BIOME_TO_BLOCK_WRAPPER.containsKey(biome))
			{
				BIOME_TO_BLOCK_WRAPPER.put(biome, newPair);
			}
			else
			{
				BIOME_TO_BLOCK_WRAPPER.compute(biome, (existingBiome,existingPair) ->
				{
					if (existingPair == null
						|| existingPair.count < newPair.count)
					{
						//// don't log if the block is the same
						//if (!existingPair.blockStateWrapper.equals(newPair.blockStateWrapper))
						//{
						//	LOGGER.info("["+existingBiome.getSerialString()+"] Replacing [" + existingPair + "] with [" + newPair + "]");
						//}
						
						return newPair;
					}
					
					return existingPair;
				});
			}
		}
		
		
		
		BlockCountPair pair = this.getMostCommonBlockForBiome(biomeBlockCounts, biomeWrapper);
		if (pair != null)
		{
			BIOME_TO_BLOCK_WRAPPER.putIfAbsent(biomeWrapper, pair);
		}
		
		BlockCountPair foundBlockPair = BIOME_TO_BLOCK_WRAPPER.get(biomeWrapper);
		if (foundBlockPair == null)
		{
			return BlockStateWrapper.getDirtBlockStateWrapper(this.serverLevelWrapper);
		}
		
		return foundBlockPair.blockStateWrapper;
	}
	
	@Nullable
	private BlockCountPair getMostCommonBlockForBiome(
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
		
		return new BlockCountPair(mostCommonBlock, highestCount);
	}
	
	//endregion
	
	
	
	//=====================//
	// noise surface logic //
	//=====================//
	//region
	
	private static int findSurfaceHeight(DensityFunction finalDensity, ILevelWrapper levelWrapper, int blockX, int blockZ)
	{
		// stat notes:
		// each are with 24 cores for DH
		// 128 render distance
		// terralith world
		
		
		//// 15.9 million // 37 sec
		//// this is the most accurate but also the slowest (especially for extended height worlds)
		//return findSurfaceHeightMarching(finalDensity, levelWrapper, blockX, blockZ, NO_HEIGHT_HINT);
		
		
		//// 3.3 million // 23 sec
		//// this is the fastest but most likely to have incorrect height if overhangs exist
		//return binarySearchSurfaceHeight(finalDensity, levelWrapper, blockX, blockZ);
		
		
		// 5.3 million // 27 sec
		// middle ground between binary search for best-case scenarios
		// and marching for accuracy
		int candidate = binarySearchSurfaceHeight(finalDensity, levelWrapper, blockX, blockZ);
		
		if (sanityCheckSurface(finalDensity, levelWrapper, blockX, blockZ, candidate))
		{
			return candidate;
		}
		
		// fall back to the slower, anomaly-aware marching approach
		return findSurfaceHeightMarching(finalDensity, levelWrapper, blockX, blockZ);
	}
	
	private static int binarySearchSurfaceHeight(
		DensityFunction finalDensity,
		ILevelWrapper levelWrapper,
		int blockX, int blockZ)
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
	
	private static int findSurfaceHeightMarching(
		DensityFunction finalDensity,
		ILevelWrapper levelWrapper,
		int blockX, int blockZ)
	{
		int top = levelWrapper.getMaxHeight();
		int bottom = levelWrapper.getMinHeight();
		int prevY = top;
		int y = (top - MARCH_STEP);
		
		while (y >= bottom)
		{
			if (isSolid(finalDensity, blockX, y, blockZ))
			{
				return binaryRefine(finalDensity, blockX, blockZ, prevY, y) + 1;
			}
			else
			{
				prevY = y;
				y -= MARCH_STEP;
			}
		}
		
		// should only happen on empty worlds (ie the end)
		return levelWrapper.getMinHeight();
	}
	
	// TODO rename
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
	
	private static boolean sanityCheckSurface(
		DensityFunction finalDensity, ILevelWrapper levelWrapper,
		int blockX, int blockZ, int candidateSurfaceY)
	{
		int bottom = levelWrapper.getMinHeight();
		int solidTopY = candidateSurfaceY - 1; // binary search's solidY, i.e. the actual top solid block
		
		for (int depth : SANITY_CHECK_DEPTHS)
		{
			int checkY = solidTopY - depth;
			if (checkY < bottom)
			{
				break; // ran out of world to check, treat as fine
			}
			
			if (!isSolid(finalDensity, blockX, checkY, blockZ))
			{
				return false; // hit air again below — this was likely a thin floating blob, not real ground
			}
		}
		
		return true;
	}
	
	
	// TODO rename
	private static boolean isSolid(DensityFunction finalDensity, int blockX, int blockY, int blockZ)
	{ return finalDensity.compute(new DensityFunction.SinglePointContext(blockX, blockY, blockZ)) > 0.0; }
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	@Override 
	public void close()
	{
		// nothing currently needed
	}
	
	//endregion
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	private static class BlockCountPair
	{
		public final IBlockStateWrapper blockStateWrapper;
		public final int count;
		
		public BlockCountPair(IBlockStateWrapper blockStateWrapper, int count)
		{
			this.blockStateWrapper = blockStateWrapper;
			this.count = count;
		}
		
		@Override
		public String toString()
		{
			// count first for easier reading with long block serials
			return this.count + " - " + this.blockStateWrapper.getSerialString();
		}
		
	}
	
	//endregion
	
	
	
}
