package com.seibel.distanthorizons.common.wrappers.misc;

import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IMutableBlockPosWrapper;
#if MC_VER <= MC_1_12_2
import net.minecraft.util.math.BlockPos;
#else
import net.minecraft.core.BlockPos;
#endif

public class MutableBlockPosWrapper implements IMutableBlockPosWrapper
{
	public final BlockPos.MutableBlockPos pos;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public MutableBlockPosWrapper()
	{
		this.pos = new BlockPos.MutableBlockPos(); 
	}
	
	//endregion
	
	
	
	//===========//
	// overrides //
	//===========//
	//region
	
	@Override 
	public Object getWrappedMcObject() { return this.pos; }
	
	//endregion
	
	
	
}
