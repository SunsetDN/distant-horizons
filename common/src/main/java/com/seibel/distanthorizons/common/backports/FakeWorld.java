package com.seibel.distanthorizons.common.backports;

#if MC_VER <= MC_1_7_10

import com.seibel.distanthorizons.common.wrappers.block.ClientBlockStateColorCache;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Used by {@link ClientBlockStateColorCache}
 * in order to handle some world related operations.
 */
public class FakeWorld implements IBlockAccess
{
	private IBlockAccess realWorld;
	private BiomeGenBase biome;
	
	private int blockX;
	private int blockY;
	private int blockZ;
	
	private FakeBlockState blockState;
	
	
	
	//========//
	// update //
	//========//
	//region
	
	public void update(
		IBlockAccess realWorld, BiomeGenBase biome, 
		int blockX, int blockY, int blockZ, 
		FakeBlockState blockState)
	{
		this.realWorld = realWorld;
		this.biome = biome;
		
		this.blockX = blockX;
		this.blockY = blockY;
		this.blockZ = blockZ;
		
		this.blockState = blockState;
		if (this.biome == null)
		{
			this.biome = BiomeGenBase.plains; // Fallback to prevent null pointers
		}
	}
	
	//endregion
	
	
	
	//===========//
	// overrides //
	//===========//
	//region
	
	@Override
	public Block getBlock(int x, int y, int z)
	{
		if (x == this.blockX 
			&& y == this.blockY 
			&& z == this.blockZ)
		{
			return this.blockState.block;
		}
		
		return Blocks.air;
	}

	@Override
	public TileEntity getTileEntity(int x, int y, int z) { return null; }

	@Override
	public int getLightBrightnessForSkyBlocks(int x, int y, int z, int min) { return 0; }

	@Override
	public int getBlockMetadata(int x, int y, int z)
	{
		if (x == this.blockX 
			&& y == this.blockY 
			&& z == this.blockZ)
		{
			return this.blockState.meta;
		}
		else if (this.blockState.block instanceof BlockDoublePlant)
		{
			return 2; // TODO
		}
		
		return 0;
	}

	@Override
	public int isBlockProvidingPowerTo(int x, int y, int z, int directionIn) { return 0; }

	@Override
	public boolean isAirBlock(int x, int y, int z) { return false; }

	@Override
	public BiomeGenBase getBiomeGenForCoords(int x, int z)
	{
		// Not 100% accurate since grass samples the surrounding blocks, but good enough for now
		return this.biome;
	}

	@Override
	public int getHeight() { return this.realWorld.getHeight(); }

	@Override
	public boolean extendedLevelsInChunkCache() { return this.realWorld.extendedLevelsInChunkCache(); }

	@Override
	public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) { throw new UnsupportedOperationException("isSideSolid() isn't implemented"); }
	
	//endregion
	
	
	
}

#endif
