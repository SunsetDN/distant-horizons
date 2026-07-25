package com.seibel.distanthorizons.common.backports;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public interface IDhBlockStateBackport
{
	Material getMaterial();
	
	Block getBlock();
	
	int getMeta();
	
	int getLightValue();
	
}
