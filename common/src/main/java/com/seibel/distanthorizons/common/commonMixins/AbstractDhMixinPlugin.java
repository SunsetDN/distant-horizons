package com.seibel.distanthorizons.common.commonMixins;

import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IImmersivePortalsAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IModChecker;

public abstract class AbstractDhMixinPlugin
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private boolean firstRun = false;
	private boolean isNeoforgeMixinFile;
	
	
	
	/**
	 * @param targetClassName example: "net.minecraft.world.level.chunk.ChunkGenerator"
	 * @param mixinClassName example: "com.seibel.distanthorizons.neoforge.mixins.server.MixinChunkGenerator"
	 */
	public boolean shouldApplyDhMixin(String targetClassName, String mixinClassName)
	{
		if (!this.shouldApplyDhMixinInnerLogic(targetClassName, mixinClassName))
		{
			LOGGER.debug("Skipping DH mixin ["+mixinClassName+"] -> ["+targetClassName+"] due to DH logic.");
			return false;
		}
		
		LOGGER.debug("Applying DH mixin ["+mixinClassName+"] -> ["+targetClassName+"]...");
		return true;
	}
	private boolean shouldApplyDhMixinInnerLogic(String targetClassName, String mixinClassName)
	{
		if (mixinClassName.endsWith("MixinImmersivePortalsRenderStates"))
		{
			boolean immersivePortalsPresent = false;
			try
			{
				Thread.currentThread()
					.getContextClassLoader()
					.loadClass(IImmersivePortalsAccessor.INJECTION_CLASS);
				immersivePortalsPresent = true;
			}
			catch (ClassNotFoundException ignore) { }
			
			if (!immersivePortalsPresent)
			{
				try
				{
					Thread.currentThread()
						.getContextClassLoader()
						.loadClass(IImmersivePortalsAccessor.INJECTION_CLASS_1_16);
					immersivePortalsPresent = true;
				}
				catch (ClassNotFoundException ignore) { }
			}
			
			return immersivePortalsPresent;
		}
		
		return true;
	}
	
	
	public boolean isNeoforge()
	{
		if (!this.firstRun)
		{
			// this check is necessary to prevent neoforge mixins
			// from running on forge and vice versa
			try
			{
				Class<?> cls = Class.forName("net.neoforged.fml.common.Mod"); // Check if a NeoForge exclusive class exists
				this.isNeoforgeMixinFile = true;
			}
			catch (ClassNotFoundException e)
			{
				this.isNeoforgeMixinFile = false;
			}
		}
		
		return this.isNeoforgeMixinFile;
	}
	
	
}
