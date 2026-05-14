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

import com.seibel.distanthorizons.cleanroom.modAccessor.ModChecker;
import com.seibel.distanthorizons.common.AbstractModInitializer;
import com.seibel.distanthorizons.common.commands.CommandInitializer;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.InternalServerGenerator;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IPluginPacketSender;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IModChecker;
import com.seibel.distanthorizons.coreapi.ModInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;


import java.util.List;
import java.util.function.Consumer;

/**
 * Initialize and setup the Mod. <br>
 * If you are looking for the real start of the mod
 * check out the ClientProxy.
 */
@Mod(modid = ModInfo.ID, name = ModInfo.NAME, version = ModInfo.VERSION)
public class CleanroomMain extends AbstractModInitializer
{
	@Mod.Instance
	public static CleanroomMain instance;
	
	@Mod.EventHandler
	public void preinit(FMLPreInitializationEvent event)
	{
		Configurator.setLevel("org.sqlite", Level.INFO);
		ForgeChunkManager.setForcedChunkLoadingCallback(CleanroomMain.instance, (tickets, world) -> { });	
	}
	
	@Mod.EventHandler
	public void init(FMLInitializationEvent event)
	{
		if (FMLCommonHandler.instance().getEffectiveSide().isClient())
		{
			this.onInitializeClient();
		}
		else
		{
			this.onInitializeServer();
		}
	}
	
	@Override
	protected void createInitialSharedBindings()
	{
		SingletonInjector.INSTANCE.bind(IModChecker.class, ModChecker.INSTANCE);
		SingletonInjector.INSTANCE.bind(IPluginPacketSender.class, new CleanroomPluginPacketSender());
	}
	@Override
	protected void createInitialClientBindings() { /* no additional setup needed currently */ }
	
	@Override
	protected IEventProxy createClientProxy() { return new CleanroomClientProxy(); }
	
	@Override
	protected IEventProxy createServerProxy(boolean isDedicated) { return new CleanroomServerProxy(isDedicated); }
	
	@Override
	protected void initializeModCompat()
	{
		
	}
	
/*	@Override
	protected void subscribeRegisterCommandsEvent(Consumer<CommandDispatcher<CommandSourceStack>> eventHandler)
	{ MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> { eventHandler.accept(e.getDispatcher()); }); }*/
	
	@Override
	protected void subscribeClientStartedEvent(Runnable eventHandler)
	{
		// Just run the event handler, since there are no proper ClientLifecycleEvent for the client 
		// to signify readiness other than FmlClientSetupEvent
		eventHandler.run();
	}
	
	@Mod.EventHandler
	public void onServerStarting(FMLServerStartingEvent event)
	{
		event.registerServerCommand(CommandInitializer.initCommands());
	}
	
	@Mod.EventHandler
	public void onServerAboutToStart(FMLServerAboutToStartEvent event)
	{
		if (eventHandlerStartServer != null)
		{
			eventHandlerStartServer.accept(event.getServer());
		}
	}
	
	Consumer<MinecraftServer> eventHandlerStartServer;
	
	@Override
	protected void subscribeServerStartingEvent(Consumer<MinecraftServer> eventHandler)
	{
		eventHandlerStartServer = eventHandler;
	}
	
	@Override
	protected void runDelayedSetup() { SingletonInjector.INSTANCE.runDelayedSetup(); }
	
	// ServerWorldLoadEvent
	@Mod.EventHandler
	public void dedicatedWorldLoadEvent(FMLServerAboutToStartEvent event)
	{
		ServerApi.INSTANCE.serverLoadEvent(event.getServer().isDedicatedServer());
	}
	
	// ServerWorldUnloadEvent
	@Mod.EventHandler
	public void serverWorldUnloadEvent(FMLServerStoppingEvent event)
	{
		ServerApi.INSTANCE.serverUnloadEvent();
	}
}
