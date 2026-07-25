package com.seibel.distanthorizons.common.backports;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/**
 * Name must match the 1.12 object "IBlockState".
 * This is done to combine 1.12 and 1.7 block handling in our pre-processors.
 */
public interface IBlockState
{
	Material getMaterial();
	
	Block getBlock();
	
	/** kinda like the state on MC 1.16+ blocks, but stored as an int */
	int getMeta();
	
	int getLightValue();
	
}
