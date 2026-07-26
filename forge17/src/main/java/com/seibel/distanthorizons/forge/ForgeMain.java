/*
 * This file is part of the Distant Horizons mod
 * licensed under the GNU LGPL v3 License.
 * Copyright (C) 2020 James Seibel
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.forge;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.seibel.distanthorizons.forge.wrappers.modAccessor.*;
import com.seibel.distanthorizons.forge.wrappers.modCompat.AngelicaCompat;
import com.seibel.distanthorizons.forge.wrappers.modCompat.GregTechCompat;
import com.seibel.distanthorizons.forge.wrappers.modCompat.RPLECompat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

import com.seibel.distanthorizons.common.AbstractModInitializer;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IPluginPacketSender;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IModChecker;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.common.versioning.VersionParser;
import cpw.mods.fml.common.versioning.VersionRange;
import cpw.mods.fml.relauncher.Side;
import org.jetbrains.annotations.Nullable;

/**
 * Initialize and setup the Mod. <br>
 * If you are looking for the real start of the mod
 * check out the ClientProxy.
 */
@Mod(modid = "distanthorizons", name = "DistantHorizons", dependencies = "after:angelica;")
public class ForgeMain extends AbstractModInitializer
{

    public static final String ANGELICA_MOD_ID = "angelica";
    public static final String MINIMUM_ANGELICA_VERSION = "2.1.5";
    public static final VersionRange SUPPORTED_ANGELICA_RANGE = VersionParser
        .parseRange("[" + MINIMUM_ANGELICA_VERSION + ",)");

    @Mod.Instance
    public static Object instance;

    private static final boolean DISABLE_SERVER_FOR_TESTING = false;

    public static boolean isHodgePodgeInstalled = false;
	@Nullable
    public static GregTechCompat gregTechCompat = null;
	@Nullable
    public static AngelicaCompat angelicaCompat = null;
	@Nullable
    public static RPLECompat rpleCompat = null;
	
	private Consumer<MinecraftServer> eventHandlerStartServer;
	
	
	
	//=======//
	// setup //
	//=======//
	//region
	
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) 
    {
        if (FMLCommonHandler.instance()
            .getEffectiveSide()
            .isClient()) 
		{
            this.onInitializeClient();
			
			// greg tech
            if (Loader.isModLoaded("gregtech") 
	            && this.enableGregTechAccessor())
            {	
                gregTechCompat = new GregTechCompat();
            }
			
			// angelica
            if (Loader.isModLoaded(ANGELICA_MOD_ID) 
	            && event.getSide() == Side.CLIENT) 
			{
                angelicaCompat = new AngelicaCompat();
                angelicaCompat.throwIfUnsupportedAngelicaVersion();
            }
			
			// RPLE
            if (Loader.isModLoaded("rple")) 
			{
                rpleCompat = new RPLECompat();
            }
        } 
		else if (!DISABLE_SERVER_FOR_TESTING) 
		{
            this.onInitializeServer();
        }
		
        ForgeChunkManager.setForcedChunkLoadingCallback(
            instance,
            (List<ForgeChunkManager.Ticket> tickets, World world) -> chunkLoadedCallback());
    }
	private boolean enableGregTechAccessor()
	{
		try
		{
			Class.forName("gregtech.api.interfaces.IBlockWithTextures");
			return true;
		}
		catch (ClassNotFoundException e)
		{
			return false;
		}
	}
	private void chunkLoadedCallback() { }
	
	@NetworkCheckHandler
	public boolean checkNetwork(Map<String, String> map, Side side)
	{
		if (side == Side.SERVER)
		{
			isHodgePodgeInstalled = map.containsKey("hodgepodge");
		}
		
		return true;
	}
	
	//endregion
	
	
	
	//=====================//
	// server world events //
	//=====================//
	//region
	
	@Mod.EventHandler
	public void onServerAboutToStart(FMLServerAboutToStartEvent event)
	{
		if (DISABLE_SERVER_FOR_TESTING)
		{
			return;
		}
		
		if (this.eventHandlerStartServer != null)
		{
			this.eventHandlerStartServer.accept(event.getServer());
		}
	}
	
	// ServerWorldLoadEvent
    @Mod.EventHandler
    public void dedicatedWorldLoadEvent(FMLServerAboutToStartEvent event)
    {
	    if (DISABLE_SERVER_FOR_TESTING)
	    {
		    return;
	    }
		
	    ServerApi.INSTANCE.serverLoadEvent(
		    event.getServer()
			    .isDedicatedServer());
    }
	
    // ServerWorldUnloadEvent
    @Mod.EventHandler
    public void serverWorldUnloadEvent(FMLServerStoppingEvent event) 
    {
        if (DISABLE_SERVER_FOR_TESTING) 
		{
            return;
        }
		
        ServerApi.INSTANCE.serverUnloadEvent();
    }
	
    @Mod.EventHandler
    public void serverWorldUnloadEvent(FMLServerStoppedEvent event) 
    {
        if (DISABLE_SERVER_FOR_TESTING) 
		{
            return;
        }
		
        ForgeServerProxy.serverStopping();
    }
	
	//endregion
	
	
	
	//========================//
	// AbstractModInitializer //
	//========================//
	//region
	
    @Override
    protected void createInitialSharedBindings() 
    {
        SingletonInjector.INSTANCE.bind(IModChecker.class, ModChecker.INSTANCE);
        SingletonInjector.INSTANCE.bind(IPluginPacketSender.class, new ForgePluginPacketSender());
    }
	
	@Override
	protected void createInitialClientBindings() { }
	@Override
    protected IEventProxy createClientProxy() { return new ForgeClientProxy(); }
	
    @Override
    protected IEventProxy createServerProxy(boolean isDedicated) {  return new ForgeServerProxy(isDedicated); }
	
    @Override
    protected void initializeModCompat() 
    { this.tryCreateModCompatAccessor("angelica", IIrisAccessor.class, IrisAccessor::new); }
	
    @Override
    protected void subscribeClientStartedEvent(Runnable eventHandler) { eventHandler.run(); }
	
	@Override
	protected void subscribeServerStartingEvent(Consumer<MinecraftServer> eventHandler)
	{ this.eventHandlerStartServer = eventHandler; }
	
	@Override
	protected void runDelayedSetup() { SingletonInjector.INSTANCE.runDelayedSetup(); }
	
	//region
	
	
	
}
