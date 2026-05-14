/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.cleanroom;

import com.seibel.distanthorizons.common.AbstractModInitializer;
import com.seibel.distanthorizons.common.util.ProxyUtil;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.logging.f3.F3Screen;
import com.seibel.distanthorizons.core.network.messages.AbstractNetworkMessage;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;

import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IPluginPacketSender;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL32;

import java.util.concurrent.AbstractExecutorService;

public class CleanroomClientProxy implements AbstractModInitializer.IEventProxy
{
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final CleanroomPluginPacketSender PACKET_SENDER = (CleanroomPluginPacketSender) SingletonInjector.INSTANCE.get(IPluginPacketSender.class);
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static World GetEventLevel(WorldEvent e) { return e.getWorld(); }
	
	
	@Override
	public void registerEvents()
	{
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(FMLCommonHandler.instance());
		CleanroomPluginPacketSender.setPacketHandler(ClientApi.INSTANCE::pluginMessageReceived);
	}
	
	
	
	//==============//
	// world events //
	//==============//
	
	@SubscribeEvent
	public void clientLevelLoadEvent(WorldEvent.Load event)
	{
		LOGGER.info("level load");
		
		World level = event.getWorld();
		if (!(level instanceof WorldClient clientLevel))
		{
			return;
		}
		
		IClientLevelWrapper clientLevelWrapper = ClientLevelWrapper.getWrapper(clientLevel, true);
		ClientApi.INSTANCE.clientLevelLoadEvent(clientLevelWrapper);
	}
	
	@SubscribeEvent
	public void clientLevelUnloadEvent(WorldEvent.Unload event)
	{
		LOGGER.info("level unload");
		
		World level = event.getWorld();
		if (!(level instanceof WorldClient clientLevel))
		{
			return;
		}
		
		IClientLevelWrapper clientLevelWrapper = ClientLevelWrapper.getWrapper(clientLevel);
		ClientApi.INSTANCE.clientLevelUnloadEvent(clientLevelWrapper);
	}
	
	
	
	//==============//
	// chunk events //
	//==============//
	//region
	
	@SubscribeEvent
	public void rightClickBlockEvent(PlayerInteractEvent.RightClickBlock event)
	{
		if (MC.clientConnectedToDedicatedServer())
		{
			World level = event.getWorld();
			
			ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(level);
			if (SharedApi.isChunkAtBlockPosAlreadyUpdating(wrappedLevel, event.getPos().getX(), event.getPos().getZ()))
			{
				return;
			}
			
			AbstractExecutorService executor = ThreadPoolUtil.getFileHandlerExecutor();
			if (executor != null)
			{
				executor.execute(() ->
				{
					Chunk chunk = level.getChunk(event.getPos());
					SharedApi.INSTANCE.applyChunkUpdate(new ChunkWrapper(chunk, wrappedLevel), wrappedLevel);
				});
			}
		}
	}
	@SubscribeEvent
	public void leftClickBlockEvent(PlayerInteractEvent.LeftClickBlock event)
	{
		if (MC.clientConnectedToDedicatedServer())
		{
			World level = event.getWorld();
			
			ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(level);
			if (SharedApi.isChunkAtBlockPosAlreadyUpdating(wrappedLevel, event.getPos().getX(), event.getPos().getZ()))
			{
				return;
			}
			
			AbstractExecutorService executor = ThreadPoolUtil.getFileHandlerExecutor();
			if (executor != null)
			{
				executor.execute(() ->
				{
					Chunk chunk = level.getChunk(event.getPos());
					SharedApi.INSTANCE.applyChunkUpdate(new ChunkWrapper(chunk, wrappedLevel), wrappedLevel);
				});
			}
		}
	}

	@SubscribeEvent
	public void clientChunkLoadEvent(ChunkEvent.Load event)
	{
		if (MC.clientConnectedToDedicatedServer())
		{
			ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(GetEventLevel(event));
			IChunkWrapper chunkWrapper = new ChunkWrapper(event.getChunk(), wrappedLevel);
			SharedApi.INSTANCE.applyChunkUpdate(chunkWrapper, wrappedLevel);
		}
	}
	
	//endregion
	
	
	
	//==============//
	// key bindings //
	//==============//
	//region
	
	@SubscribeEvent
	public void registerKeyBindings(InputEvent.KeyInputEvent event)
	{
	/*	if (Minecraft.getMinecraft().player == null)
		{
			return;
		}
		if (event.getAction() != GLFW.GLFW_PRESS)
		{
			return;
		}
		
		ClientApi.INSTANCE.keyPressedEvent(event.getKey());*/
	}
	
	//endregion
	
	
	
	//===========//
	// rendering //
	//===========//
	//region
	
	@SubscribeEvent
	public void afterLevelRenderEvent(TickEvent.RenderTickEvent event)
	{
		if (event.type.equals(TickEvent.RenderTickEvent.Type.RENDER))
		{
			try
			{
				// should generally only need to be set once per game session
				// allows DH to render directly to Optifine's level frame buffer,
				// allowing better shader support
				MinecraftRenderWrapper.INSTANCE.finalLevelFrameBufferId = GL32.glGetInteger(GL32.GL_FRAMEBUFFER_BINDING);
			}
			catch (Exception | Error e)
			{
				LOGGER.error("Unexpected error in afterLevelRenderEvent: "+e.getMessage(), e);
			}
		}
	}
	
	@SubscribeEvent
	public void onRenderOverlay(RenderGameOverlayEvent.Text event) 
	{
		Minecraft mc = Minecraft.getMinecraft();
		if (event.isCanceled() 
			|| !mc.gameSettings.showDebugInfo)
		{
			return;
		}
		
		F3Screen.addStringToDisplay(event.getRight());
	}
	
	//endregion
	
	
	
}
