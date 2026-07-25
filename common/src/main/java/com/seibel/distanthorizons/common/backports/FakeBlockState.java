package com.seibel.distanthorizons.common.backports;

#if MC_VER <= MC_1_7_10

import java.util.Objects;

import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.forge.ForgeMain;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/**
 * Allows for smoother use of blocks in
 * {@link IBlockStateWrapper}
 */
public class FakeBlockState implements IBlockState
{
	public final Block block;
	public final int meta;
	private final int hashCode;
	
	
	
	//==============//
	// constructors //
	//==============//
	//region
	
	public FakeBlockState(Block block, int meta) { this(block, meta, Block.getIdFromBlock(block)); }
	public FakeBlockState(Block block, int meta, int blockId)
	{
		this.block = block;
		this.meta = meta;
		this.hashCode = calculateHashCode(blockId, meta);
	}
	
	//endregion
	
	
	
	//=========//
	// getters //
	//=========//
	//region
	
	@Override
	public Material getMaterial() { return this.block.getMaterial(); }
	
	@Override
	public Block getBlock() { return this.block; }
	
	@Override
	public int getMeta() { return this.meta; }
	
	@Override
	public int getLightValue() { return getLightEmission(this.block, this.meta); }
	public static int getLightEmission(Block block, int meta)
	{
		if (ForgeMain.rpleCompat != null)
		{
			return ForgeMain.rpleCompat.getColor(block, meta);
		}
		
		return Math.min(15, block.getLightValue());
	}
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof FakeBlockState))
		{
			return false;
		}
		
		FakeBlockState that = (FakeBlockState) obj;
		return this.meta == that.meta 
			&& Objects.equals(this.block, that.block);
	}
	
	@Override
	public int hashCode() { return this.hashCode; }
	public static int calculateHashCode(int blockId, int meta) { return (blockId << 16) + meta; }
	
	//endregion
	
	
	
}

#endif
