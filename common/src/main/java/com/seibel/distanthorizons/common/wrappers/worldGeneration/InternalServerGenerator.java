package com.seibel.distanthorizons.common.wrappers.worldGeneration;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.modAccessor.IHodgePodgeCommonAccessor;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.params.GlobalWorldGenParams;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.api.internal.chunkUpdating.ChunkUpdateQueueManager;
import com.seibel.distanthorizons.core.api.internal.chunkUpdating.WorldChunkUpdateManager;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.enums.MinecraftTextFormat;
import com.seibel.distanthorizons.core.generation.DhLightingEngine;
import com.seibel.distanthorizons.core.level.IDhServerLevel;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.ExceptionUtil;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.TimerUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IC2meAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.modLoader.IForgeMain;
import com.seibel.distanthorizons.core.wrapperInterfaces.modLoader.IForgeServerProxy;
import com.seibel.distanthorizons.coreapi.ModInfo;

import org.jetbrains.annotations.Nullable;
#if MC_VER <= MC_1_7_10
import com.seibel.distanthorizons.common.backports.ChunkPos;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.ForgeChunkManager;
#elif MC_VER <= MC_1_12_2
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
#else
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
#endif

#if MC_VER <= MC_1_12_2
#elif MC_VER <= MC_1_20_4
import net.minecraft.world.level.chunk.ChunkStatus;
#else
import net.minecraft.world.level.chunk.status.ChunkStatus;
#endif

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

#if MC_VER <= MC_1_7_10
import java.lang.reflect.Field;
#endif

public class InternalServerGenerator
{
	public static final DhLogger LOGGER = new DhLoggerBuilder()
			.name("LOD World Gen - Internal Server")
			.fileLevelConfig(Config.Common.Logging.logWorldGenEventToFile)
			.build();
	
	public static final DhLogger CHUNK_LOAD_LOGGER = new DhLoggerBuilder()
			.name("LOD Chunk Loading")
			.fileLevelConfig(Config.Common.Logging.logWorldGenChunkLoadEventToFile)
			.build();
	
	#if MC_VER <= MC_1_7_10
	private static final IForgeServerProxy FORGE_SERVER_PROXY = SingletonInjector.INSTANCE.get(IForgeServerProxy.class);
	private static final IForgeMain FORGE_MAIN = SingletonInjector.INSTANCE.get(IForgeMain.class);
	#else
	#endif
	
	private static final IC2meAccessor C2ME_ACCESSOR = ModAccessorInjector.INSTANCE.get(IC2meAccessor.class);
	private static final IHodgePodgeCommonAccessor HODGE_PODGE_ACCESSOR = ModAccessorInjector.INSTANCE.get(IHodgePodgeCommonAccessor.class);
	
	/**
	 * Used to revert the ignore logic in {@link SharedApi} so
	 * that a given chunk pos can be handled again.
	 * A timer is used so we don't have to inject into MC's code and it works sell enough
	 * most of the time.
	 * If a chunk does get through due the timeout not being long enough that isn't the end of the world.
	 */
	private static final int MS_TO_IGNORE_CHUNK_AFTER_COMPLETION = 5_000;
	
	#if MC_VER <= MC_1_12_2
	#elif MC_VER < MC_1_21_5
	private static final TicketType<ChunkPos> DH_SERVER_GEN_TICKET = TicketType.create("dh_server_gen_ticket", Comparator.comparingLong(ChunkPos::toLong));
	#elif MC_VER < MC_1_21_9
	private static final TicketType DH_SERVER_GEN_TICKET = new TicketType(/* timeout, 0 = disabled*/0L, /* persist */ false, TicketType.TicketUse.LOADING);
	#else
	private static final TicketType DH_SERVER_GEN_TICKET = new TicketType(/* timeout, 0 = disabled*/0L, /* flags */TicketType.FLAG_LOADING);
	#endif
	
	private static boolean c2meMissingWarningLogged = false;
	
	
	private final GlobalWorldGenParams params;
	private final IDhServerLevel dhServerLevel;
	@Nullable
	private final ChunkUpdateQueueManager updateManager;
	private final Timer chunkSaveIgnoreTimer = TimerUtil.CreateTimer("ChunkSaveIgnoreTimer");
	#if MC_VER <= MC_1_7_10
	private final ForgeChunkManager.Ticket dhServerGenTicket;
	#elif MC_VER <= MC_1_12_2
	private static final java.util.concurrent.Semaphore chunkRequestSemaphore = new java.util.concurrent.Semaphore(20);
	#endif

	#if MC_VER <= MC_1_12_2
	/**
	 * Older Minecraft needs neighboring chunks loaded.
	 * This map tracks how many in-flight generation events currently need each chunk pos loaded.
	 */
	private final ConcurrentHashMap<DhChunkPos, Integer> generationChunkRefCountMap = new ConcurrentHashMap<>();
	#endif
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public InternalServerGenerator(GlobalWorldGenParams params, IDhServerLevel dhServerLevel)
	{
		this.params = params;
		this.dhServerLevel = dhServerLevel;
		this.updateManager = WorldChunkUpdateManager.INSTANCE.getByLevelWrapper(this.dhServerLevel.getServerLevelWrapper());

		#if MC_VER <= MC_1_7_10
		this.dhServerGenTicket = ForgeChunkManager.requestTicket(FORGE_MAIN, params.mcServerLevel, ForgeChunkManager.Type.NORMAL);
		increaseChunkLimit(this.dhServerGenTicket, 1000);
		#endif
	}

	#if MC_VER <= MC_1_7_10
	private static void increaseChunkLimit(ForgeChunkManager.Ticket ticket, int newMaxDepth)
	{
		try
		{
			Field maxDepthField = ticket.getClass().getDeclaredField("maxDepth");
			maxDepthField.setAccessible(true);
			maxDepthField.setInt(ticket, newMaxDepth);
		}
		catch (Exception e)
		{
			LOGGER.warn("Failed to increase Forge chunk ticket limit.", e);
		}
	}
	#endif
	
	
	
	#if MC_VER <= MC_1_12_2
	//===================================//
	// neighbor-chunk reference counting //
	//===================================//
	
	/**
	 * Marks the given pos as needed by one more generation event. <br>
	 * Must be called from the server thread.
	 *
	 * @return true if this was the first reference, meaning the chunk needs to be loaded.
	 */
	private boolean acquireChunkRef(DhChunkPos chunkPos)
	{
		Integer refCount = this.generationChunkRefCountMap.get(chunkPos);
		int newRefCount = (refCount == null) ? 1 : (refCount + 1);
		this.generationChunkRefCountMap.put(chunkPos, newRefCount);
		return newRefCount == 1;
	}
	
	/**
	 * Marks the given pos as no longer needed by one generation event. <br>
	 * Must be called from the server thread.
	 *
	 * @return true if this was the last reference, meaning the chunk can be released.
	 */
	private boolean releaseChunkRef(DhChunkPos chunkPos)
	{
		Integer refCount = this.generationChunkRefCountMap.get(chunkPos);
		if (refCount == null)
		{
			// could happen during shutdown
			CHUNK_LOAD_LOGGER.debug("Chunk ["+chunkPos+"] was released without being acquired.");
			return false;
		}
		
		if (refCount > 1)
		{
			this.generationChunkRefCountMap.put(chunkPos, refCount - 1);
			return false;
		}
		
		this.generationChunkRefCountMap.remove(chunkPos);
		return true;
	}
	
	/** @return true if no generation event currently needs the given pos loaded. */
	private boolean chunkRefCountIsZero(DhChunkPos chunkPos) { return !this.generationChunkRefCountMap.containsKey(chunkPos); }
	#endif
	
	
	
	//============//
	// generation //
	//============//
	
	public void generateChunksViaInternalServer(GenerationEvent genEvent)
	{
		this.runValidation();
		
		try
		{
			//=====================//
			// create gen requests //
			//=====================//
			
			#if MC_VER <= MC_1_12_2
			ArrayList<CompletableFuture<Chunk>> getChunkFutureList = new ArrayList<>();
			#else
			ArrayList<CompletableFuture<ChunkAccess>> getChunkFutureList = new ArrayList<>();
			#endif
			
			{
				#if MC_VER == MC_1_12_2
				while (!isServerHealthy())
				{
					try
					{
						// Don't submit request until server tps is healthy
						Thread.sleep(50);
					}
					catch (InterruptedException e)
					{
						throw new CancellationException("Interrupted while waiting for server");
					}
				}
				#endif
				
				Iterator<ChunkPos> chunkPosIterator = ChunkPosGenStream.getIterator(genEvent.minPos.getX(), genEvent.minPos.getZ(), genEvent.widthInChunks, 0);
				while (chunkPosIterator.hasNext())
				{
					ChunkPos chunkPos = chunkPosIterator.next();
					
					#if MC_VER == MC_1_12_2
					chunkRequestSemaphore.acquireUninterruptibly();
					#endif
					
					#if MC_VER <= MC_1_12_2
					CompletableFuture<Chunk> requestChunkFuture;
					#else
					CompletableFuture<ChunkAccess> requestChunkFuture;
					#endif
					
					requestChunkFuture =
						this.requestChunkFromServerAsync(chunkPos)
							// log errors if necessary
							.whenCompleteAsync(
								(chunk, throwable) ->
								{
									#if MC_VER == MC_1_12_2
									chunkRequestSemaphore.release();
									#endif
									// unwrap the CompletionException if necessary
									Throwable actualThrowable = throwable;
									while (actualThrowable instanceof CompletionException)
									{
										actualThrowable = actualThrowable.getCause();
									}
									
									if (actualThrowable != null)
									{
										// ignore expected shutdown exceptions
										boolean isShutdownException =
											ExceptionUtil.isShutdownException(actualThrowable)
											|| actualThrowable.getMessage().contains("Unloaded chunk");
										if (!isShutdownException)
										{
											CHUNK_LOAD_LOGGER.warn("DistantHorizons: Couldn't load chunk [" + chunkPos + "] from server, error: [" + actualThrowable.getMessage() + "].", actualThrowable);
										}
									}
								});
					
					getChunkFutureList.add(requestChunkFuture);
				}
			}
			
			
			
			//==============================//
			// wait for generation requests //
			//==============================//
			
			// Join-ing each thread will prevent DH from working on anything else
			// but will also prevent over-queuing world gen tasks.
			// If C2ME is present the CPU will still be well utilized.
			
			ArrayList<IChunkWrapper> chunkWrappers = new ArrayList<>();
			for (int i = 0; i < getChunkFutureList.size(); i++)
			{
				#if MC_VER <= MC_1_12_2
				CompletableFuture<Chunk> getChunkFuture;
				Chunk chunk;
				#else
				CompletableFuture<ChunkAccess> getChunkFuture;
				ChunkAccess chunk;
				#endif
				
				getChunkFuture = getChunkFutureList.get(i);
				chunk = getChunkFuture.join();
				if (chunk != null)
				{
					ChunkWrapper chunkWrapper = new ChunkWrapper(chunk, this.dhServerLevel.getLevelWrapper());
					chunkWrapper.createDhHeightMaps();
					chunkWrappers.add(chunkWrapper);
				}
			}
			
			
			
			//==========================//
			// process generated chunks //
			//==========================//
			
			int maxSkyLight = this.dhServerLevel.getServerLevelWrapper().hasSkyLight() ? LodUtil.MAX_MC_LIGHT : LodUtil.MIN_MC_LIGHT;
			for (int i = 0; i < chunkWrappers.size(); i++)
			{
				ChunkWrapper chunkWrapper = (ChunkWrapper)chunkWrappers.get(i);
				
				// pre-generated chunks should have lighting but new ones won't
				if (!chunkWrapper.isDhBlockLightingCorrect())
				{
					DhLightingEngine.INSTANCE.bakeChunkBlockLighting(chunkWrapper, chunkWrappers, maxSkyLight);
				}
				
				this.dhServerLevel.updateBeaconBeamsForChunk(chunkWrapper, chunkWrappers);
				genEvent.resultConsumer.accept(chunkWrapper);
			}
		}
		finally
		{
			ArrayList<CompletableFuture<Void>> releaseFutures = new ArrayList<>();
			#if MC_VER == MC_1_12_2
			Set<ChunkPos> neighborIgnoreChunkPosSet = new HashSet<>();
			#endif
			
			// release all chunks from the server to prevent out of memory issues
			Iterator<ChunkPos> chunkPosIterator = ChunkPosGenStream.getIterator(genEvent.minPos.getX(), genEvent.minPos.getZ(), genEvent.widthInChunks, 0);
			while (chunkPosIterator.hasNext())
			{
				ChunkPos chunkPos = chunkPosIterator.next();
				releaseFutures.add(this.releaseChunkFromServerAsync(this.params.mcServerLevel, chunkPos));
        
                #if MC_VER == MC_1_12_2
				// collect unique neighbor positions for release and ignore removal
				for (int dx = -1; dx <= 1; dx++)
				{
					for (int dz = -1; dz <= 1; dz++)
					{
						if (dx == 0 && dz == 0)
						{
							continue;
						}
						
						neighborIgnoreChunkPosSet.add(new ChunkPos(chunkPos.x + dx, chunkPos.z + dz));
					}
				}
                #endif
			}
    
            #if MC_VER == MC_1_12_2
			// release neighbor chunks that were loaded in requestChunkFromServerAsync
			for (ChunkPos neighborPos : neighborIgnoreChunkPosSet)
			{
				releaseFutures.add(this.releaseChunkFromServerAsync(this.params.mcServerLevel, neighborPos));
			}
            #endif
			
			// wait for all release futures to finish to prevent an issue where DH queues
			// tickets faster than MC can clear them out
			for (int i = 0; i < releaseFutures.size(); i++)
			{
				CompletableFuture<Void> releaseFuture = releaseFutures.get(i);
				releaseFuture.join();
			}
			
			// tick after all unloads are queued
            #if MC_VER == MC_1_12_2
			CompletableFuture<Void> tickFuture = new CompletableFuture<>();
			this.params.mcServerLevel.getMinecraftServer().addScheduledTask(() ->
			{
				try
				{
					ChunkProviderServer provider = this.params.mcServerLevel.getChunkProvider();
					while (!provider.droppedChunks.isEmpty())
					{
						provider.tick();
					}
				}
				finally
				{
					tickFuture.complete(null);
				}
			});
			tickFuture.join();
            #endif
    
            #if MC_VER == MC_1_12_2
			for (ChunkPos neighborPos : neighborIgnoreChunkPosSet)
			{
				this.chunkSaveIgnoreTimer.schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						if (InternalServerGenerator.this.updateManager != null)
						{
							InternalServerGenerator.this.updateManager.removePosToIgnore(McObjectConverter.convert(neighborPos));
						}
					}
				}, MS_TO_IGNORE_CHUNK_AFTER_COMPLETION);
			}
            #endif
		}
	}
	private void runValidation()
	{
		// DH thread check
		if (!DhApi.isDhThread()
			&& ModInfo.IS_DEV_BUILD)
		{
			throw new IllegalStateException("Internal server generation should be called from one of DH's world gen thread. Current thread: ["+Thread.currentThread().getName()+"]");
		}
		
		#if MC_VER > MC_1_12_2
		// C2ME present?
		if (C2ME_ACCESSOR == null
			&& !c2meMissingWarningLogged)
		{
			c2meMissingWarningLogged = true;
			
			String c2meWarning = "C2ME missing, \n" +
				"low CPU usage and slow world gen speeds expected. \n" +
				"DH is set to use MC's internal server for world gen \n" +
				"this mode is less efficient unless a mod like C2ME is present."
				;
			
			if (Config.Common.Logging.Warning.showSlowWorldGenSettingWarnings.get())
			{
				String message =
					MinecraftTextFormat.ORANGE + "Distant Horizons: slow world gen." + MinecraftTextFormat.CLEAR_FORMATTING + "\n" +
						c2meWarning;
				ClientApi.INSTANCE.queueChatMessage(message);
			}
			
			LOGGER.warn(c2meWarning);
		}
		#endif
	}

	private void scheduleRemovePosToIgnore(DhChunkPos chunkPos)
	{
		this.chunkSaveIgnoreTimer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				if (InternalServerGenerator.this.updateManager != null)
				{
					InternalServerGenerator.this.updateManager.removePosToIgnore(chunkPos);
				}
			}
		}, MS_TO_IGNORE_CHUNK_AFTER_COMPLETION);
	}

	#if MC_VER <= MC_1_12_2
	private CompletableFuture<Chunk> requestChunkFromServerAsync(ChunkPos chunkPos)
	#else
	private CompletableFuture<ChunkAccess> requestChunkFromServerAsync(ChunkPos chunkPos)
	#endif
	{
		#if MC_VER <= MC_1_12_2
		{
			WorldServer level = this.params.mcServerLevel;
			
			// ignore chunk update events for this position
			if (this.updateManager != null)
			{
				this.updateManager.addPosToIgnore(McObjectConverter.convert(chunkPos));
			}
			
			#if MC_VER <= MC_1_7_10
			CompletableFuture<Chunk> future = FORGE_SERVER_PROXY.scheduleTickTask(true, () ->
			#else
			CompletableFuture<Chunk> future = new CompletableFuture<>();
			level.getMinecraftServer().addScheduledTask(() ->
			#endif
			{
				#if MC_VER <= MC_1_7_10
				ChunkProviderServer provider = (ChunkProviderServer) level.getChunkProvider();
				if (HODGE_PODGE_ACCESSOR != null)
				{
					HODGE_PODGE_ACCESSOR.preventChunkSimulation(level, chunkPos.x, chunkPos.z);
				}
				ForgeChunkManager.forceChunk(this.dhServerGenTicket, new ChunkCoordIntPair(chunkPos.x, chunkPos.z));
				#else
				ChunkProviderServer provider = level.getChunkProvider();
				#endif
				
				// load neighbors first so the target chunk can fully populate
				for (int dx = -1; dx <= 1; dx++)
				{
					for (int dz = -1; dz <= 1; dz++)
					{
						if (dx == 0 && dz == 0) continue;
						if (this.updateManager != null)
						{
							this.updateManager.addPosToIgnore(new DhChunkPos(chunkPos.x + dx, chunkPos.z + dz));
						}

						#if MC_VER <= MC_1_7_10
						if (HODGE_PODGE_ACCESSOR != null)
						{
							HODGE_PODGE_ACCESSOR.preventChunkSimulation(level, chunkPos.x + dx, chunkPos.z + dz);
						}
						ForgeChunkManager.forceChunk(this.dhServerGenTicket, new ChunkCoordIntPair(chunkPos.x + dx, chunkPos.z + dz));
						if (!provider.chunkExists(chunkPos.x + dx, chunkPos.z + dz))
						{
							provider.loadChunk(chunkPos.x + dx, chunkPos.z + dz);
						}
						#else
						if (provider.getLoadedChunk(chunkPos.x + dx, chunkPos.z + dz) == null)
						{
							provider.provideChunk(chunkPos.x + dx, chunkPos.z + dz);
						}
						#endif
					}
				}
				
				#if MC_VER <= MC_1_7_10
				return provider.loadChunk(chunkPos.x, chunkPos.z);
				#else
				Chunk chunk = provider.provideChunk(chunkPos.x, chunkPos.z);
				future.complete(chunk);
				#endif
			});
			
			return future;
		}
		#else
		{
			return CompletableFuture.supplyAsync(() ->
			{
				ServerLevel level = this.params.mcServerLevel;
				
				// ignore chunk update events for this position
				if (this.updateManager != null)
				{
					this.updateManager.addPosToIgnore(McObjectConverter.convert(chunkPos));
				}
				
				#if MC_VER < MC_1_21_5
				int chunkLevel = 33; // 33 is equivalent to FULL Chunk
				level.getChunkSource().distanceManager.addTicket(DH_SERVER_GEN_TICKET, chunkPos, chunkLevel, chunkPos);
				#else
				level.getChunkSource().addTicketWithRadius(DH_SERVER_GEN_TICKET, chunkPos, 0);
				#endif
				
				// probably not the most optimal to run updates here, but fast enough
				level.getChunkSource().distanceManager.runAllUpdates(level.getChunkSource().chunkMap);
				
				ChunkHolder chunkHolder = level.getChunkSource().chunkMap
					.getUpdatingChunkIfPresent(
						#if MC_VER <= MC_1_21_11 chunkPos.toLong() #else chunkPos.pack() #endif
					);
				if (chunkHolder == null)
				{
					throw new IllegalStateException("No chunk chunkHolder for pos ["+chunkPos+"] after ticket has been added.");
				}
				
				// Note: ChunkStatus.FEATURES would be slightly faster than FULL, but can cause issues
				// with other mods where they need lighting/full chunk data.
				#if MC_VER <= MC_1_20_4
				return chunkHolder.getOrScheduleFuture(ChunkStatus.FULL, level.getChunkSource().chunkMap)
						.thenApply(result -> result.left().orElseThrow(() -> new RuntimeException(result.right().get().toString()))); // can throw if the server is shutting down
				#elif MC_VER <= MC_1_20_6
				return chunkHolder.getOrScheduleFuture(ChunkStatus.FULL, level.getChunkSource().chunkMap)
						.thenApply(result -> result.orElseThrow(() -> new RuntimeException(result.toString()))); // can throw if the server is shutting down
				#else
				return chunkHolder.scheduleChunkGenerationTask(ChunkStatus.FULL, level.getChunkSource().chunkMap)
						.thenApply(result -> result.orElseThrow(() -> new RuntimeException(result.getError()))); // can throw if the server is shutting down
				#endif
				
			}, this.params.mcServerLevel.getChunkSource().chunkMap.mainThreadExecutor)
			.thenCompose(Function.identity());
		}
		#endif
	}
	/**
	 * mitigates out of memory issues in the vanilla chunk system. <br>
	 * See: https://github.com/pop4959/Chunky/pull/383
	 */
	#if MC_VER <= MC_1_12_2
	private CompletableFuture<Void> releaseChunkFromServerAsync(WorldServer level, ChunkPos chunkPos)
	#else
	private CompletableFuture<Void> releaseChunkFromServerAsync(ServerLevel level, ChunkPos chunkPos)
	#endif
	{
		#if MC_VER <= MC_1_7_10
		return FORGE_SERVER_PROXY.scheduleTickTask(false, () ->
		{
			ChunkProviderServer provider = (ChunkProviderServer) level.getChunkProvider();
			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dz = -1; dz <= 1; dz++)
				{
					int x = chunkPos.x + dx;
					int z = chunkPos.z + dz;
					ForgeChunkManager.unforceChunk(this.dhServerGenTicket, new ChunkCoordIntPair(x, z));
					if (HODGE_PODGE_ACCESSOR != null)
					{
						HODGE_PODGE_ACCESSOR.allowChunkSimulation(level, x, z);
					}
					if (!level.getPlayerManager().func_152621_a(x, z))
					{
						// TODO should unloadChunksIfNotNearSpawn be implemented for MC 1.7.10?
						throw new UnsupportedOperationException("should unloadChunksIfNotNearSpawn be implemented for MC 1.7.10?");
						//provider.unloadChunksIfNotNearSpawn(x, z);
					}
					this.scheduleRemovePosToIgnore(new DhChunkPos(x, z));
				}
			}

			return null;
		});
		#else
		CompletableFuture<Void> removeTicketFuture = new CompletableFuture<>();
		#if MC_VER <= MC_1_12_2
		level.getMinecraftServer().addScheduledTask(() ->
		#else
		level.getChunkSource().chunkMap.mainThreadExecutor.execute(() ->
		#endif
		{
			try
			{
				#if MC_VER <= MC_1_12_2
				ChunkProviderServer provider = level.getChunkProvider();
				
				Chunk chunk = provider.getLoadedChunk(chunkPos.x, chunkPos.z);
				if (chunk != null)
				{
					provider.queueUnload(chunk);
				}
				#elif MC_VER < MC_1_21_5
				int chunkLevel = 33; // 33 is equivalent to FULL Chunk
				level.getChunkSource().distanceManager.removeTicket(DH_SERVER_GEN_TICKET, chunkPos, chunkLevel, chunkPos);
				#else
				level.getChunkSource().removeTicketWithRadius(DH_SERVER_GEN_TICKET, chunkPos, 0);
				#endif
				
				#if MC_VER > MC_1_12_2
				level.getChunkSource().chunkMap.tick(() -> false);
				#endif
				
				#if MC_VER > MC_1_16_5
				level.entityManager.tick();
				#endif
				
				
				// give MC a few seconds to save the chunk before
				// we can process update events there again
				this.chunkSaveIgnoreTimer.schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						if (InternalServerGenerator.this.updateManager != null)
						{
							InternalServerGenerator.this.updateManager.removePosToIgnore(McObjectConverter.convert(chunkPos));
						}
					}
				}, MS_TO_IGNORE_CHUNK_AFTER_COMPLETION);
				
			}
			catch (Exception e)
			{
				LOGGER.warn("Failed to release chunk ["+chunkPos+"] back to internal server. Error: ["+e.getMessage()+"]", e);
			}
			finally
			{
				removeTicketFuture.complete(null);
			}
		});
		return removeTicketFuture;
		#endif
	}
	
	
	
	//======//
	// misc //
	//======//
	
	#if MC_VER == MC_1_12_2
	private boolean isServerHealthy()
	{
		if(this.params.mcServerLevel.getMinecraftServer() == null) 
		{
	        return false; 
	    }
		
		long[] ticks = this.params.mcServerLevel.getMinecraftServer().tickTimeArray;
		long[] sorted = ticks.clone();
		Arrays.sort(sorted);
		
		int p99Index = (int)Math.ceil(0.99 * sorted.length) - 1;
		double p99Ms  = sorted[Math.max(0, p99Index)] * 1e-6;
		double avgMs  = Arrays.stream(sorted).average().orElse(0) * 1e-6;
		
		return avgMs < 20.0 && p99Ms < 50.0;
	}
	#endif
	
}
