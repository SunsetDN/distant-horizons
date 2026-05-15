package com.seibel.distanthorizons.common.wrappers.minecraft;

import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import net.minecraft.server.dedicated.DedicatedServer;
import org.jetbrains.annotations.Nullable;

#if MC_VER > MC_1_12_2
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
#else
import net.minecraft.world.WorldServer;
#endif

#if  MC_VER <= MC_1_12_2
#elif  MC_VER <= MC_1_21_10
import net.minecraft.resources.ResourceLocation;
#else
import net.minecraft.resources.Identifier;
#endif

#if  MC_VER > MC_1_19_2
import net.minecraft.core.registries.Registries;
#elif MC_VER > MC_1_12_2
import net.minecraft.core.Registry;
#endif

import java.io.File;

public class MinecraftServerWrapper implements IMinecraftSharedWrapper
{
	public static final MinecraftServerWrapper INSTANCE = new MinecraftServerWrapper();
	
	/** set during server startup */
	@Nullable
	public DedicatedServer dedicatedServer = null;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	private MinecraftServerWrapper() { }
	
	
	
	//=========//
	// methods //
	//=========//
	
	@Override
	public boolean isDedicatedServer() { return true; }
	
	@Override
	public File getInstallationDirectory()
	{
		if (this.dedicatedServer == null)
		{
			throw new IllegalStateException("Trying to get Installation Direction before dedicated server completed initialization!");
		}
		
		#if MC_VER <= MC_1_12_2
		return this.dedicatedServer.getDataDirectory();
		#elif MC_VER < MC_1_21_1
		return this.dedicatedServer.getServerDirectory();
		#else
		return this.dedicatedServer.getServerDirectory().toFile();
		#endif
	}
	
	@Override
	public int getPlayerCount() 
	{
		if (this.dedicatedServer == null)
		{
			throw new IllegalStateException("Trying to get player count before dedicated server completed initialization!");
		}
		
		#if MC_VER <= MC_1_12_2
		return this.dedicatedServer.getCurrentPlayerCount();
		#else
		return this.dedicatedServer.getPlayerCount();
		#endif
	}
	
	
	
	@Override
	public IServerLevelWrapper getWrappedServerLevel(String levelKey)
	{
		#if  MC_VER <= MC_1_12_2
		int dimensionID;
		try
		{
			dimensionID = Integer.parseInt(levelKey);
		}
		catch (NumberFormatException ignored)
		{
			return null;
		}
		#else
		#if  MC_VER <= MC_1_21_10
		ResourceLocation levelID = ResourceLocation.tryParse(levelKey);
		#else
		Identifier levelID = Identifier.tryParse(levelKey);
		#endif
		if (levelID == null) return null;
		
		#if  MC_VER > MC_1_19_2
		ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, levelID);
		#else
		ResourceKey<Level> resourceKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, levelID);
		#endif
		#endif
		
		#if MC_VER > MC_1_12_2
		ServerLevel level = dedicatedServer.getLevel(resourceKey);
		#else
		WorldServer level = dedicatedServer.getWorld(dimensionID);
		#endif
		return ServerLevelWrapper.getWrapper(level);
	}
	
}
