
package com.seibel.distanthorizons.forgearchaic.wrappers.modCompat;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import com.mitchej123.hodgepodge.SimulationDistanceHelper;

public class HodgePodgeCompat 
{
	public static void preventChunkSimulation(World world, int x, int z)
	{ SimulationDistanceHelper.preventChunkSimulation(world, ChunkCoordIntPair.chunkXZ2Int(x, z), true); }
	
	public static void allowChunkSimulation(World world, int x, int z) 
    { SimulationDistanceHelper.preventChunkSimulation(world, ChunkCoordIntPair.chunkXZ2Int(x, z), false); }
	
	
	
}
