package com.seibel.distanthorizons.forge;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.seibel.distanthorizons.common.commonMixins.MixinChunkMapCommon;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.seibel.distanthorizons.common.AbstractModInitializer;
import com.seibel.distanthorizons.common.util.ProxyUtil;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.misc.ServerPlayerWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ForgeServerProxy implements AbstractModInitializer.IEventProxy {

	
	private static final ConcurrentLinkedQueue<ScheduledTask<?>> TASK_QUEUE = new ConcurrentLinkedQueue<>();
	
	
    private final boolean isDedicated;
	
	
	
	//=============//
    // constructor //
    //=============//
	//region

    public ForgeServerProxy(boolean isDedicated) { this.isDedicated = isDedicated; }
	
	@Override
	public void registerEvents()
	{
		MinecraftForge.EVENT_BUS.register(this);
		FMLCommonHandler.instance()
			.bus()
			.register(this);
		
		if (this.isDedicated)
		{
			ForgePluginPacketSender.setPacketHandler(ServerApi.INSTANCE::pluginMessageReceived);
		}
	}
	
	//endregion
	
	
	
    //========//
    // events //
    //========//
	//region
	
	public static void serverStopping()
	{
		// clear the task queue
		while (!TASK_QUEUE.isEmpty())
		{
			ScheduledTask<?> scheduledTask = TASK_QUEUE.poll();
			if (scheduledTask == null)
			{
				continue;
			}
			
			scheduledTask.future.complete(null);
		}
	}

    // ServerTickEvent (at end)
    @SubscribeEvent
	public void serverTickEvent(TickEvent.ServerTickEvent event)
    {
		if (event.phase == TickEvent.Phase.END)
	    {
		    // limit how many tasks we can run at a time
		    // to prevent lagging the server thread
		    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(15);
		    boolean processedAtLeastOne = false;
		    while (!TASK_QUEUE.isEmpty())
		    {
			    ScheduledTask<?> scheduledTask = TASK_QUEUE.poll();
			    if (scheduledTask == null)
			    {
				    continue;
			    }
			    
			    scheduledTask.run();
			    
				// TODO why do we ignore the timeout if the current task is "limited"?
			    if (scheduledTask.limited)
			    {
				    if (!processedAtLeastOne)
				    {
					    processedAtLeastOne = true;
				    }
				    else if (System.nanoTime() >= deadline)
				    {
					    break;
				    }
			    }
		    }
		}
	}

    // ServerLevelLoadEvent
    @SubscribeEvent 
    public void serverLevelLoadEvent(WorldEvent.Load event)
    {
		if (GetEventLevel(event) instanceof WorldServer)
	    {
			ServerApi.INSTANCE.serverLevelLoadEvent(getServerLevelWrapper((WorldServer) GetEventLevel(event)));
		}
	}

    // ServerLevelUnloadEvent
    @SubscribeEvent
	public void serverLevelUnloadEvent(WorldEvent.Unload event)
    {
		if (GetEventLevel(event) instanceof WorldServer)
	    {
			// Make new server level wrapper so it's not cached...
			ServerApi.INSTANCE.serverLevelUnloadEvent(new ServerLevelWrapper((WorldServer) GetEventLevel(event)));
		}
	}

    @SubscribeEvent
	public void serverChunkLoadEvent(ChunkEvent.Load event)
    {
		if (!(event.world instanceof WorldServer))
	    {
			return;
		}
		
		Chunk chunk = event.getChunk();
		ILevelWrapper levelWrapper = ProxyUtil.getLevelWrapper(GetEventLevel(event));
		ChunkWrapper chunkWrapper = new ChunkWrapper(chunk, levelWrapper);
		// Only handle event if chunk is ready, otherwise drop the update - we'll get it later during save
		if (!chunk.isTerrainPopulated || !chunk.isLightPopulated)
	    {
			return;
		}
	    ServerApi.INSTANCE.serverChunkLoadEvent(chunkWrapper, levelWrapper);
	}

    @SubscribeEvent
    public void serverChunkSaveEvent(ChunkDataEvent.Save event) 
    {
        if (!(event.world instanceof WorldServer)) 
		{
            return;
        }
	    MixinChunkMapCommon.onChunkSave((WorldServer) event.world, event.getChunk());
    }

    @SubscribeEvent
    public void playerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event) 
    { ServerApi.INSTANCE.serverPlayerJoinEvent(getServerPlayerWrapper(event)); }

    @SubscribeEvent
    public void playerLoggedOutEvent(PlayerEvent.PlayerLoggedOutEvent event) 
    { ServerApi.INSTANCE.serverPlayerDisconnectEvent(getServerPlayerWrapper(event)); }

    @SubscribeEvent
    public void playerChangedDimensionEvent(PlayerEvent.PlayerChangedDimensionEvent event) 
    {
        ServerApi.INSTANCE.serverPlayerLevelChangeEvent(
            getServerPlayerWrapper(event),
            getServerLevelWrapper(event.fromDim, event),
            getServerLevelWrapper(event.toDim, event));
    }

    @SubscribeEvent
	public void clickBlockEvent(PlayerInteractEvent event)
    {
		ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(event.world);
		if (SharedApi.isChunkAtBlockPosAlreadyUpdating(wrappedLevel, event.x, event.z))
	    {
			return;
		}
		
		scheduleTickTask(false, () ->
		{
			Chunk chunk = event.world.getChunkFromBlockCoords(event.x, event.z);
			ChunkWrapper chunkWrapper = new ChunkWrapper(chunk, wrappedLevel);
			SharedApi.INSTANCE.applyChunkUpdate(chunkWrapper, wrappedLevel, true);
			return null;
		});
	}
	
	//endregion
	
	
	
    //================//
    // helper methods //
    //================//
	//region
	
	// Schedule a task that runs on the main thread and returns a CompletableFuture result
	public static <T> CompletableFuture<T> scheduleTickTask(boolean limited, Supplier<T> task)
	{
		CompletableFuture<T> future = new CompletableFuture<>();
		TASK_QUEUE.add(new ScheduledTask<>(task, future, limited));
		return future;
	}
	
	private static World GetEventLevel(WorldEvent e) { return e.world; }
	
	private static IServerLevelWrapper getServerLevelWrapper(WorldServer level) 
    { return ServerLevelWrapper.getWrapper(level); }
	
    private static IServerLevelWrapper getServerLevelWrapper(int dim, PlayerEvent event) 
    {
        WorldServer world = (WorldServer) event.player.worldObj;
        WorldServer worldDim = world.func_73046_m()
            .worldServerForDimension(dim);
        return getServerLevelWrapper(worldDim);
    }
	
    private static IServerPlayerWrapper getServerPlayerWrapper(PlayerEvent event) 
    { return ServerPlayerWrapper.getWrapper((EntityPlayerMP) event.player); }
	
	//endregion
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	private static class ChunkLoadEvent
	{
		public final ChunkWrapper chunkWrapper;
		public final ILevelWrapper level;
		
		/** 
		 * used to track how long this event has
		 * been queued so we can clean up old tasks.
		 */
		public int numberOfTicksSinceQueue = 0;
		
		
		
		private ChunkLoadEvent(ChunkWrapper chunkWrapper, ILevelWrapper level)
		{
			this.chunkWrapper = chunkWrapper;
			this.level = level;
		}
	}
	
	private static class ScheduledTask<T>
	{
		private final Supplier<T> task;
		private final CompletableFuture<T> future;
		
		// TODO what does this mean?
		public final boolean limited;
		
		
		public ScheduledTask(Supplier<T> task, CompletableFuture<T> future, boolean limited)
		{
			this.task = task;
			this.future = future;
			this.limited = limited;
		}
		
		
		public void run()
		{
			try
			{
				this.future.complete(this.task.get());
			}
			catch (Exception e)
			{
				this.future.completeExceptionally(e);
			}
		}
	}
	
	//endregion
	
	
	
}
