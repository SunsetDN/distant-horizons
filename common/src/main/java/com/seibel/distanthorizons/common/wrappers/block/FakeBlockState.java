package com.seibel.distanthorizons.common.wrappers.block;

#if MC_VER <= MC_1_7_10

import java.util.Objects;

import net.minecraft.block.Block;

public class FakeBlockState
{
	public final Block block;
	public final int meta;
	private final int hashCode;
	
	public FakeBlockState(Block block, int meta) { this(block, meta, Block.getIdFromBlock(block)); }
	
	public FakeBlockState(Block block, int meta, int blockId)
	{
		this.block = block;
		this.meta = meta;
		this.hashCode = calculateHashCode(blockId, meta);
	}
	
	public static int calculateHashCode(int blockId, int meta) { return (blockId << 16) + meta; }
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof FakeBlockState))
		{
			return false;
		}
		
		FakeBlockState that = (FakeBlockState) obj;
		return this.meta == that.meta && Objects.equals(this.block, that.block);
	}
	
	@Override
	public int hashCode() { return this.hashCode; }
}

#endif
