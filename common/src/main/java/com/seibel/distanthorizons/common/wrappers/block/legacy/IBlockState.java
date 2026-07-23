package com.seibel.distanthorizons.common.wrappers.block.legacy;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public interface IBlockState
{
	Material getMaterial();
	
	Block getBlock();
	
	int getMeta();
	
	int getLightValue();
	
}
