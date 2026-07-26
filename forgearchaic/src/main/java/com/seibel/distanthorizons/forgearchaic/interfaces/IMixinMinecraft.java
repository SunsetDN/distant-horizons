package com.seibel.distanthorizons.forgearchaic.interfaces;

import net.minecraft.util.Timer;

public interface IMixinMinecraft 
{
	/** needed to get the frame time */
    Timer getTimer();
}
