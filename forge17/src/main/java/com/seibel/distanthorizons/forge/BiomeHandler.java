package com.seibel.distanthorizons.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.seibel.distanthorizons.mixin.MixinBiomeGenBase;
import net.minecraft.world.biome.BiomeGenBase;

/**
 * 
 */
public class BiomeHandler
{
	private static final List<BiomeGenBase> BIOME_LIST = new ArrayList<>();
	private static ConcurrentHashMap<String, BiomeGenBase> biomeByName;
	
	
	
	/** 
	 * Called by {@link MixinBiomeGenBase} to
	 * populate all possible options.
	 */
	public static void addBiome(BiomeGenBase biome) { BIOME_LIST.add(biome); }
	
	public static BiomeGenBase getBiomeByName(String name)
	{
		// lazy initialization since we need to wait for BIOME_LIST
		// to finish populating before we can put together the map
		if (biomeByName == null)
		{
			// May be called from multiple threads, causing the
			// map to be created multiple times.
			// Not a big deal since the ending Concurrent Map should be the same either way.
			ConcurrentHashMap<String, BiomeGenBase> newBiomeByName = new ConcurrentHashMap<>();
			for (BiomeGenBase biome : BIOME_LIST)
			{
				newBiomeByName.put(biome.biomeName, biome);
			}
			biomeByName = newBiomeByName;
		}
		
		return biomeByName.get(name);
	}
	
	
	
}
